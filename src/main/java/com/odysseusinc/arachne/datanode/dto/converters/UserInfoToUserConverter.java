/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin
 * Created: July 15, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.model.user.User;
import org.ohdsi.authenticator.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class UserInfoToUserConverter implements Converter<UserInfo, User>{

    @Autowired
    public UserInfoToUserConverter(GenericConversionService conversionService){

        conversionService.addConverter(this);
    }

    @Override
    public User convert(UserInfo source) {

        User user = new User();

        user.setUsername(source.getUser().getUsername());
        user.setFirstName(source.getUser().getFirstname());
        user.setLastName(source.getUser().getLastname());
        user.setEmail(source.getUser().getEmail());
        return user;
    }
}
