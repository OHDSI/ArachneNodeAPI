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

public class CredentialsValidationContext {

    private DBMSType type;
    private String username;
    private String usernameField;
    private String keyfileField;
    private Object keyfile;
    private boolean useKerberos;
    private KerberosAuthMechanism kerberosAuthMechanism;
    private String kerberosUser;
    private String kerberosUserField;

    public CredentialsValidationContext(DBMSType type) {

        this.type = type;
    }

    public DBMSType getType() {

        return type;
    }

    public String getUsername() {

        return username;
    }

    public String getKeyfileField() {

        return keyfileField;
    }

    void setKeyfileField(String keyfileField) {

        this.keyfileField = keyfileField;
    }

    public Object getKeyfile() {

        return keyfile;
    }

    void setKeyfile(Object keyfile) {

        this.keyfile = keyfile;
    }

    public String getUsernameField() {

        return usernameField;
    }

    void setUsername(String username) {

        this.username = username;
    }

    void setUsernameField(String usernameField) {

        this.usernameField = usernameField;
    }

    public boolean isUseKerberos() {

        return useKerberos;
    }

    void setUseKerberos(boolean useKerberos) {

        this.useKerberos = useKerberos;
    }

    public KerberosAuthMechanism getKerberosAuthMechanism() {

        return kerberosAuthMechanism;
    }

    void setKerberosAuthMechanism(KerberosAuthMechanism kerberosAuthMechanism) {

        this.kerberosAuthMechanism = kerberosAuthMechanism;
    }

    public String getKerberosUser() {

        return kerberosUser;
    }

    void setKerberosUser(String kerberosUser) {

        this.kerberosUser = kerberosUser;
    }

    public String getKerberosUserField() {

        return kerberosUserField;
    }

    void setKerberosUserField(String kerberosUserField) {

        this.kerberosUserField = kerberosUserField;
    }
}
