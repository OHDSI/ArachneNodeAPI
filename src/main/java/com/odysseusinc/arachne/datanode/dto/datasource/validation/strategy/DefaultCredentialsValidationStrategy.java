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
 * Created: Oct 1, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.datasource.validation.strategy;

import com.odysseusinc.arachne.datanode.dto.datasource.validation.context.CredentialsValidationContext;
import java.util.Objects;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class DefaultCredentialsValidationStrategy extends BaseCredentialsValidationStrategy {

    @Override
    public boolean isValid(ConstraintValidatorContext context, CredentialsValidationContext validationContext) {

        if (StringUtils.isBlank(validationContext.getUsername())) {
            buildFieldConstraint(context, validationContext.getUsernameField());
            return false;
        }
        return true;
    }

}
