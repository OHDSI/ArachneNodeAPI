/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
 * Created: November 01, 2016
 *
 */

package com.odysseusinc.arachne.datanode.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Holds the info for a authenticated user (Principal)
 *
 * @author pascal alma
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String token;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, String username, String token, Collection<? extends GrantedAuthority> authorities) {

        this.id = id;
        this.username = username;
        this.token = token;
        this.authorities = authorities;
    }

    @JsonIgnore
    public Long getId() {

        return id;
    }

    @Override
    public String getUsername() {

        return username;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {

        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {

        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {

        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {

        return true;
    }

    public String getToken() {

        return token;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return authorities;
    }

    @Override
    public String getPassword() {

        return null;
    }

}
