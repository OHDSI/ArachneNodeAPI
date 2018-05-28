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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: August 25, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.encoders;

import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.form.FormDataProcessor;
import feign.form.FormEncoder;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultipartEncoder extends FormEncoder {

    private final Encoder deligate;

    private final Map<String, FormDataProcessor> processors;

    public MultipartEncoder() {

        this(new Encoder.Default());
    }

    public MultipartEncoder(Encoder delegate) {

        this(delegate, new SpringMultipartEncodedDataProcessor());
    }

    public MultipartEncoder(Encoder delegate, FormDataProcessor processor) {

        this.deligate = delegate;
        processors = new HashMap<>();
        processors.put(processor.getSupportetContentType().toLowerCase(),
                processor);
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {

        if (!MAP_STRING_WILDCARD.equals(bodyType)) {
            deligate.encode(object, bodyType, template);
            return;
        }

        String formType = "";
        for (Map.Entry<String, Collection<String>> entry : template.headers().entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("Content-Type")) {
                continue;
            }
            for (String contentType : entry.getValue()) {
                if (contentType != null && processors.containsKey(contentType.toLowerCase())) {
                    formType = contentType;
                    break;
                }
            }
            if (!formType.isEmpty()) {
                break;
            }
        }

        if (formType.isEmpty()) {
            deligate.encode(object, bodyType, template);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) object;
        processors.get(formType).process(data, template);
    }
}
