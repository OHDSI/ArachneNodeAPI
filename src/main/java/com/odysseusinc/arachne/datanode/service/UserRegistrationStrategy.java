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
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Aug 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.datanode.model.user.User;

/**
 * {@code UserRegisterStrategy} determines how to register user after successful authentication.
 *
 * @see com.odysseusinc.arachne.datanode.service.impl.CreateIfFirstUserRegistrationStrategy
 */
@FunctionalInterface
public interface UserRegistrationStrategy {
    String CREATE_IF_FIRST = "CREATE_IF_FIRST";
    String CREATE_IF_NOT_EXISTS = "CREATE_IF_NOT_EXISTS";

    User registerUser(User user);
}
