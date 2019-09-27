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
 * Created: Sep 27, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.datasource.validation;

import com.odysseusinc.arachne.commons.types.DBMSType;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseValidator {

    protected boolean isValid(ConstraintValidatorContext context, String username, DBMSType dbmsType, String fieldName) {

        if (StringUtils.isBlank(username) && !DBMSType.BIGQUERY.equals(dbmsType)) {
            buildFieldConstraint(context, fieldName);
            return false;
        }
        return true;
    }

    protected void buildFieldConstraint(ConstraintValidatorContext context, String fieldName) {

        context.disableDefaultConstraintViolation();
        String tmpl = context.getDefaultConstraintMessageTemplate();
        context.buildConstraintViolationWithTemplate(tmpl)
                .addPropertyNode(fieldName)
                .addConstraintViolation();
    }
}
