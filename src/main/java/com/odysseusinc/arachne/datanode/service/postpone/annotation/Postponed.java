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
 * Created: Jul 25, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service.postpone.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>{@literal @}Postponed used to mark methods that communicates with Portal.
 * When Portal isn't accessible then Data Node is running in stand-alone mode.
 * Stand-alone mode means some operations couldn't execute immediately and
 * should be postponed until Portal remains to accept requests.</p>
 *
 * <p>Postponed method have to indicate <b>action</b> that should be unique across class or interface.
 * When method returns some value it's possible to set default value that would be returned when running in the
 * stand-alone mode. defaultReturnValue can contains SpEl expression that provides flexible configuration of services.</p>
 *
 * <p>When postponed request failed it can retry.</p>
 * <p>Retrying interval is controlled by the application parameter 'postponed.retry.interval' set in milliseconds.</p>
 * <p>Maximum of retry attempts is set by the application parameter 'postponed.retry.maxAttempts`, when is set to 0 means unlimited attempts.</p>
 *
 * <pre>{@code
 * interface Service {
 *  @literal @Postponed(action="create", defaultReturnValue="new com.package.DataSource()")
 *    DataSource create();
 * }
 * }
 * </pre>
 *
 * <p></p>When method accepts arguments then it's critical to be able to serialize each argument with Jackson.
 * It's because method's arguments stored to the database.</p>
 * <p>In the case when method signature contains parameters could not be proceed by Jackson, e.g. entities, then
 * corresponding parameter have to be marked with {@code @literal @PostponedArgument} to provide serializer and
 * deserializer.</p>
 * <p>Postponed argument serializer/deserializer is just the same as used in controllers to provide DTO objects.</p>
 * <pre>{@code
 *     @literal @Postponed(action = "update")
 *      void updateOnCentral(@PostponedArgument(serializer = UserToUserDTOConverter.class,
 *             deserializer = UserDTOToUserConverter.class) User user);
 * }</pre>
 *
 * @see com.odysseusinc.arachne.datanode.service.postpone.annotation.PostponedArgument
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Postponed {
    String action();
    String defaultReturnValue() default "null";
}
