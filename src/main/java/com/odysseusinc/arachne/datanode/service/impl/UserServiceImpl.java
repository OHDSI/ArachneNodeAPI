/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: December 19, 2016
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static com.odysseusinc.arachne.datanode.security.RolesConstants.ROLE_ADMIN;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.user.Role;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.repository.RoleRepository;
import com.odysseusinc.arachne.datanode.repository.UserRepository;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.UserService;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String USER_NOT_FOUND_EXCEPTION = "User with id='$s' is not found";
    private static final String ROLE_ADMIN_IS_NOT_FOUND_EXCEPTION = "ROLE_ADMIN is not found";
    private static final String RELINKING_ALL_USERS_LOG = "Relinking all DataNode users on Central";
    private static final String RELINKING_ALL_USERS_ERROR_LOG = "Relinking users on Central error: {}";
    private static final String ADDING_USER_FROM_CENTRAL_LOG = "Adding User from central with centralUserId='{}'";
    private static final String REMOVING_USER_LOG = "Removing user with id='{}'";

    @Autowired
    protected GenericConversionService conversionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BaseCentralIntegrationService centralIntegrationService;
    @Autowired
    private DataNodeService dataNodeService;

    @PostConstruct
    public void syncUsers() {

        try {
            dataNodeService.findCurrentDataNode().ifPresent(dataNode -> {
                        LOG.info(RELINKING_ALL_USERS_LOG);
                        final List<User> users = userRepository.findAll();
                        centralIntegrationService.relinkAllUsersToDataNodeOnCentral(dataNode, users);
                    }
            );
        } catch (Exception ex) {
            LOG.error(RELINKING_ALL_USERS_ERROR_LOG, ex.getMessage());
        }
    }

    @Override
    public User createUserInformation(String login, String password, String firstName, String lastName, String email,
                                      String langKey) {

        User newUser = new User();
        final Role role = roleRepository.findOne("ROLE_USER");
        final List<Role> roles = new LinkedList<>();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        roles.add(role);
        newUser.setRoles(roles);
        userRepository.save(newUser);
        LOG.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    @Override
    public User get(Long id) throws NotExistException {

        final User user = userRepository.getOne(id);
        if (user == null) {
            final String message = String.format(USER_NOT_FOUND_EXCEPTION, id);
            throw new NotExistException(message, User.class);
        }
        return user;
    }

    @Override
    public Optional<User> findByUsername(String login) {

        return userRepository.findOneByEmail(login);
    }

    @Override
    public void disableUser(String login) {

        User user = userRepository.findOneByEmail(login).orElseThrow(IllegalArgumentException::new);
        userRepository.save(user);
    }

    @Override
    public void enableUser(String login) {

        User user = userRepository.findOneByEmail(login).orElseThrow(IllegalArgumentException::new);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(String login) {

        userRepository.findOneByEmail(login).ifPresent(u -> {
            userRepository.delete(u);
            LOG.debug("Deleted User: {}", u);
        });
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return userRepository.findOneByEmail(username).map(
                user ->
                        new org.springframework.security.core.userdetails.User(
                                user.getEmail(), "",
                                user.getRoles().stream().map(
                                        role -> new SimpleGrantedAuthority(role.getName()))
                                        .collect(Collectors.toList())
                        ))
                .orElseThrow(AuthException::new);
    }

    @Override
    public void setToken(User user, String token) {

        user.setToken(token);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public User createIfFirst(String email) {

        User result = null;
        long count = userRepository.count();
        if (count == 0) {
            User user = new User();
            user.setEmail(email);
            user.setUsername(email);
            roleRepository.findFirstByName(ROLE_ADMIN).ifPresent(role -> user.getRoles().add(role));
            //TODO: goto cantral get /me info
            result = userRepository.save(user);
        }
        return result;
    }

    @Override
    public List<User> suggestNotAdmin(User user, final String query, Integer limit) {

        Set<String> emails = userRepository.findAll().stream().map(User::getEmail).collect(Collectors.toSet());
        JsonResult<List<CommonUserDTO>> result =
                centralIntegrationService.suggestUsersFromCentral(user, query, emails, limit);
        List<User> suggestedUsers = result
                .getResult()
                .stream()
                .map(dto -> conversionService.convert(dto, User.class))
                .collect(Collectors.toList());
        List<User> adminUsers = userRepository.findByRoles_name(ROLE_ADMIN, new Sort(Sort.Direction.ASC, "email"));
        return suggestedUsers
                .stream()
                .filter(u -> adminUsers.stream().noneMatch(adminUser -> adminUser.getEmail().equals(u.getEmail())))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getAllAdmins(final String sortBy, final Boolean sortAsc) {

        Sort.Direction direction = sortAsc != null && sortAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        final Sort sort;
        if (sortBy == null || sortBy.isEmpty() || sortBy.equals("name")) {
            sort = new Sort(direction, "firstName", "lastName");
        } else {
            sort = new Sort(direction, sortBy);
        }
        return userRepository.findByRoles_name(ROLE_ADMIN, sort);
    }

    @Override
    public List<User> getAllUsers(final String sortBy, final Boolean sortAsc) {

        Sort.Direction direction = sortAsc != null && sortAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        final Sort sort;
        if (sortBy == null || sortBy.isEmpty() || sortBy.equals("name")) {
            sort = new Sort(direction, "firstName", "lastName");
        } else {
            sort = new Sort(direction, sortBy);
        }
        return userRepository.findAll(sort);
    }

    @Override
    public void addUserToAdmins(User currentUser, Long id) {

        User user = findOrAddFromCentral(currentUser, id);
        final Optional<Role> firstByName = roleRepository.findFirstByName(ROLE_ADMIN);
        firstByName.ifPresent(role -> {
                    user.getRoles().add(role);
                    dataNodeService.findCurrentDataNode().ifPresent(dataNode ->
                            centralIntegrationService.linkUserToDataNodeOnCentral(dataNode, user)
                    );
                    userRepository.save(user);
                }
        );
        firstByName.orElseThrow(() -> new ArachneSystemRuntimeException(ROLE_ADMIN_IS_NOT_FOUND_EXCEPTION));
    }

    private User findOrAddFromCentral(User currentUser, Long id) {

        User user = userRepository.findOne(id);
        if (user == null) {
            user = addUserFromCentral(currentUser, id);
        }
        return user;
    }

    @Override
    public void removeUserFromAdmins(Long id) {

        User user = userRepository.findOne(id);
        final Optional<Role> firstByName = roleRepository.findFirstByName(ROLE_ADMIN);
        firstByName.ifPresent(role -> {
                    user.getRoles().remove(role);
                    dataNodeService.findCurrentDataNode().ifPresent(dataNode ->
                            centralIntegrationService.linkUserToDataNodeOnCentral(dataNode, user)
                    );
                    userRepository.save(user);
                }
        );
        firstByName.orElseThrow(() -> new ArachneSystemRuntimeException(ROLE_ADMIN_IS_NOT_FOUND_EXCEPTION));
    }

    @Override
    public void remove(Long id) throws NotExistException {

        LOG.info(REMOVING_USER_LOG, id);
        final User user = get(id);
        dataNodeService.findCurrentDataNode().ifPresent(dataNode ->
                centralIntegrationService.unlinkUserToDataNodeOnCentral(dataNode, user)
        );
        userRepository.delete(id);
    }

    @Override
    public List<CommonUserDTO> suggestUsersFromCentral(User user, String query, int limit) {

        Set<String> emails = userRepository.findAll().stream().map(User::getEmail).collect(Collectors.toSet());
        JsonResult<List<CommonUserDTO>> result =
                centralIntegrationService.suggestUsersFromCentral(user, query, emails, limit);
        return result.getResult();
    }

    @Override
    public User addUserFromCentral(User loggedUser, Long centralUserId) {

        LOG.info(ADDING_USER_FROM_CENTRAL_LOG, centralUserId);
        JsonResult<CommonUserDTO> jsonResult =
                centralIntegrationService.getUserFromCentral(loggedUser, centralUserId);
        CommonUserDTO userDTO = jsonResult.getResult();
        User savedUser = null;
        if (userDTO != null) {
            Optional<User> localuser = userRepository.findOneByEmail(userDTO.getEmail());
            if (!localuser.isPresent()) {
                final User user = conversionService.convert(userDTO, User.class);
                dataNodeService.findCurrentDataNode().ifPresent(dataNode ->
                        centralIntegrationService.linkUserToDataNodeOnCentral(
                                dataNode,
                                user
                        )
                );
                savedUser = userRepository.save(user);
            }
        }
        return savedUser;
    }

    @Override
    public User getUser(Principal principal) throws PermissionDeniedException {

        if (principal == null) {
            throw new PermissionDeniedException();
        }
        return findByUsername(principal.getName()).orElseThrow(PermissionDeniedException::new);
    }
}
