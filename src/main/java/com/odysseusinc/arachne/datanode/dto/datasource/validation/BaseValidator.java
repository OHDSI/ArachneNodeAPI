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
import com.odysseusinc.arachne.datanode.dto.datasource.validation.context.CredentialsValidationContext;
import com.odysseusinc.arachne.datanode.dto.datasource.validation.strategy.BigQueryCredentialsValidationStrategy;
import com.odysseusinc.arachne.datanode.dto.datasource.validation.strategy.CredentialsValidationStrategy;
import com.odysseusinc.arachne.datanode.dto.datasource.validation.strategy.DefaultCredentialsValidationStrategy;
import com.odysseusinc.arachne.datanode.dto.datasource.validation.strategy.ImpalaCredentialsValidationStrategy;
import java.util.HashMap;
import java.util.Map;
import javax.validation.ConstraintValidatorContext;

public abstract class BaseValidator {

    private static Map<DBMSType, CredentialsValidationStrategy> STRATEGY_MAP = new HashMap<>();

    private static CredentialsValidationStrategy DEFAULT_STRATEGY = new DefaultCredentialsValidationStrategy();

    static {
        STRATEGY_MAP.put(DBMSType.BIGQUERY, new BigQueryCredentialsValidationStrategy());
        STRATEGY_MAP.put(DBMSType.IMPALA, new ImpalaCredentialsValidationStrategy());
    }

    protected boolean isValid(ConstraintValidatorContext context, CredentialsValidationContext validationContext) {

        CredentialsValidationStrategy strategy = STRATEGY_MAP.getOrDefault(validationContext.getType(), DEFAULT_STRATEGY);
        return strategy.isValid(context, validationContext);
    }

}
