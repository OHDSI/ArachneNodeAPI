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

package com.odysseusinc.arachne.datanode.dto.datasource.validation.context;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMechanism;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class CredentialsValidationContextBuilder {

    private CredentialsValidationContext contextInstance;

    private CredentialsValidationContextBuilder(DBMSType type) {

        contextInstance = new CredentialsValidationContext(type);
    }

    public static CredentialsValidationContextBuilder newContextOfType(DBMSType type) {

        Objects.requireNonNull(type, "DBMS type should not be null");
        return new CredentialsValidationContextBuilder(type);
    }

    public CredentialsValidationContextBuilder withUsername(String fieldName, String value) {

        contextInstance.setUsernameField(fieldName);
        contextInstance.setUsername(value);
        return this;
    }

    public CredentialsValidationContextBuilder withKeyfile(String fieldName, Object value) {

        contextInstance.setKeyfileField(fieldName);
        contextInstance.setKeyfile(value);
        return this;
    }

    public CredentialsValidationContextBuilder usingKerberos(boolean useKerberos) {

        contextInstance.setUseKerberos(useKerberos);
        return this;
    }

    public CredentialsValidationContextBuilder withKerberosAuthMechanism(KerberosAuthMechanism kerberosAuthMechanism) {

        contextInstance.setKerberosAuthMechanism(kerberosAuthMechanism);
        return this;
    }

    public CredentialsValidationContextBuilder withKerberosUser(String fieldName, String value) {

        contextInstance.setKerberosUserField(fieldName);
        contextInstance.setKerberosUser(value);
        return this;
    }

    public CredentialsValidationContext build() {

        if (StringUtils.isBlank(contextInstance.getUsernameField())) {
            throw new IllegalArgumentException("usernameField is required");
        }
        return contextInstance;
    }
}
