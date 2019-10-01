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
 * Created: Sep 25, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.datasource.validation;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.validation.context.CredentialsValidationContextBuilder;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CreateDataSourceCredentialsValidator extends BaseValidator implements ConstraintValidator<ValidCredentials, CreateDataSourceDTO> {

    private static final String USERNAME_FIELDNAME = "dbUsername";
    private static final String KEYFILE_FIELDNAME = "keyfile";
    private static final String KRB_USER_FIELD = "krbUser";

    private static final MultipartFile DUMMY_FILE = new MockMultipartFile("keyfile", new byte[16]);

    @Override
    public void initialize(ValidCredentials validCredentials) {

    }

    @Override
    public boolean isValid(CreateDataSourceDTO createDataSourceDTO, ConstraintValidatorContext context) {

        return isValid(context, CredentialsValidationContextBuilder.newContextOfType(DBMSType.valueOf(createDataSourceDTO.getDbmsType()))
                .withUsername(USERNAME_FIELDNAME, createDataSourceDTO.getDbUsername())
                //Dummy file is used to omit controller incoming parameter validation since keyfile receives separately
                .withKeyfile(KEYFILE_FIELDNAME, ObjectUtils.firstNonNull(createDataSourceDTO.getKeyfile(), DUMMY_FILE))
                .usingKerberos(createDataSourceDTO.getUseKerberos())
                .withKerberosAuthMechanism(createDataSourceDTO.getKrbAuthMechanism())
                .withKerberosUser(KRB_USER_FIELD, createDataSourceDTO.getKrbUser())
                .build());
    }
}
