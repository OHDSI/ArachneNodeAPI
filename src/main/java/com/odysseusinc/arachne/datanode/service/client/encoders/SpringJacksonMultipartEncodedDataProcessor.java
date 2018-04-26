package com.odysseusinc.arachne.datanode.service.client.encoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import org.springframework.http.MediaType;

public class SpringJacksonMultipartEncodedDataProcessor extends SpringMultipartEncodedDataProcessor {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void writeParameter(PrintWriter writer, String name, Object value) {

        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(CRLF);
        writer.append("Content-Type: " + MediaType.APPLICATION_JSON_UTF8_VALUE).append(CRLF).append(CRLF);
        try {
            writer.write(mapper.writeValueAsString(value));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        writer.flush();
    }
}
