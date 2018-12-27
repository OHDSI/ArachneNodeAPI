/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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

import static feign.Util.UTF_8;

import feign.RequestTemplate;
import feign.form.FormDataProcessor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

public class SpringMultipartEncodedDataProcessor implements FormDataProcessor {

    public static final String CONTENT_TYPE;

    protected static final String CRLF;

    static {
        CONTENT_TYPE = "multipart/form-data";
        CRLF = "\r\n";
    }

    protected MultiValueMap<String, Object> prepareData(Map<String, Object> data) {

        MultiValueMap<String, Object> flattenedData = new LinkedMultiValueMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value != null && value instanceof File[]) {
                for (File file: (File[]) value) {
                    flattenedData.add(key, file);
                }
            } else if (value != null && value instanceof MultipartFile[]) {
                for (MultipartFile mpf: (MultipartFile[]) value) {
                    flattenedData.add(key, mpf);
                }
            } else {
                flattenedData.set(key, value);
            }
        }

        return flattenedData;
    }

    @Override
    public void process (Map<String, Object> data, RequestTemplate template) {
        String boundary = createBoundary();
        MultiValueMap<String, Object> preparedData = prepareData(data);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PrintWriter writer = new PrintWriter(outputStream);
            for (Map.Entry<String, List<Object>> entry : preparedData.entrySet()) {
                String key = entry.getKey();
                for (Object value: entry.getValue()) {
                    writer.append("--" + boundary).append(CRLF);

                    if (value instanceof MultipartFile) {
                        MultipartFile mpf = (MultipartFile) value;
                        writeByteArray(outputStream, writer, key, mpf.getName(), mpf.getContentType(), mpf.getBytes());
                    } else if (value instanceof File) {
                        writeFile(outputStream, writer, key, null, (File) value);
                    } else if (Objects.nonNull(value)) {
                        writeParameter(writer, entry.getKey(), value);
                    }
                    writer.append(CRLF).flush();
                }
            }
            writer.append("--" + boundary + "--").append(CRLF).flush();

            String contentType = new StringBuilder()
                    .append(CONTENT_TYPE)
                    .append("; boundary=")
                    .append(boundary)
                    .toString();

            template.header("Content-Type", contentType);
            template.body(outputStream.toByteArray(), UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String getSupportetContentType () {
        return CONTENT_TYPE;
    }

    private String createBoundary () {
        return Long.toHexString(System.currentTimeMillis());
    }

    protected void writeParameter (PrintWriter writer, String name, Object value) {
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(CRLF);
        writer.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
        writer.append(CRLF).append(value.toString());
    }

    protected void writeFile (OutputStream output, PrintWriter writer, String name, String contentType, File file) throws IOException {
        writeFileMeta(writer, name, file.getName(), contentType);
        Files.copy(file.toPath(), output);
        writer.flush();
    }

    protected void writeByteArray (OutputStream output,
                                   PrintWriter writer,
                                   String name,
                                   String originalFilename,
                                   String contentType,
                                   byte[] bytes
    ) throws IOException {
        writeFileMeta(writer, name, originalFilename, contentType);
        output.write(bytes);
        writer.flush();
    }

    private void writeFileMeta (PrintWriter writer, String name, String fileName, String contentValue) {
        String contentDesposition = new StringBuilder()
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"; ")
                .append("filename=\"").append(fileName).append("\"")
                .toString();

        if (contentValue == null) {
            contentValue = fileName != null
                    ? URLConnection.guessContentTypeFromName(fileName)
                    : "application/octet-stream";
        }
        String contentType = new StringBuilder()
                .append("Content-Type: ")
                .append(contentValue)
                .toString();

        writer.append(contentDesposition).append(CRLF);
        writer.append(contentType).append(CRLF);
        writer.append("Content-Transfer-Encoding: binary").append(CRLF);
        writer.append(CRLF).flush();
    }
}