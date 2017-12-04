/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Dec 1, 2017
 *
 */

package com.odysseusinc.arachne.datanode.model.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.jasypt.hibernate4.type.AbstractEncryptedAsStringType;

public class CheckedEncryptedStringType extends AbstractEncryptedAsStringType {
    @Override
    protected Object convertToObject(String value) {

        return value;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {

        checkInitialization();
        if (Objects.nonNull(value)){
            String encrypted = this.encryptor.encrypt(convertToString(value));
            st.setString(index, "ENC(" + encrypted + ")");
        } else {
            st.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {

        checkInitialization();
        if (rs.wasNull()){
            return null;
        } else {
            final String message = rs.getString(names[0]);
            Object result;
            if (Objects.isNull(message)) {
                return null;
            }
            if (message.startsWith("ENC(")) {
                String password = message.substring(4, message.length());
                result = convertToObject(encryptor.decrypt(password));
            } else {
                result = message;
            }
            return result;
        }
    }

    @Override
    public Class returnedClass() {

        return String.class;
    }
}
