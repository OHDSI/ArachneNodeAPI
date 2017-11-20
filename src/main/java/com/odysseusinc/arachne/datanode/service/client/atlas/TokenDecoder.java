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
 * Created: Nov 8, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.atlas;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

public class TokenDecoder implements Decoder {

    public static final String AUTH_RESPONSE_HEADER = "bearer";

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {

        if (response.status() == 401){
            throw new DecodeException("Authentication failed");
        } else if (response.status() == 404) {
            return null;
        }
        Collection<String> authHeader = response.headers().get(AUTH_RESPONSE_HEADER);
        return Objects.nonNull(authHeader) ? authHeader.iterator().next() : null;
    }
}
