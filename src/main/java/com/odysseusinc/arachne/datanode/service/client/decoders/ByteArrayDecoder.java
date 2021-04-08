package com.odysseusinc.arachne.datanode.service.client.decoders;

import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

public class ByteArrayDecoder implements Decoder {

    private final Decoder delegate;

    public ByteArrayDecoder(Decoder delegate) {

        this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {

        Collection<String> contentTypes = response.headers().getOrDefault("Content-Type", Collections.emptyList());
        if (contentTypes.stream().anyMatch(c -> Objects.equals(c, MediaType.APPLICATION_OCTET_STREAM_VALUE))) {
            if (type == byte[].class) {
                return StreamUtils.copyToByteArray(response.body().asInputStream());
            } else if (type instanceof Class && MultipartFile.class.isAssignableFrom((Class<?>) type)) {
                //TODO proceed Content-Disposition for filename, may be later if required
                throw new UnsupportedOperationException("MultipartFile is not implemented yet");
            }
        } else if (Objects.nonNull(delegate)) {
            return delegate.decode(response, type);
        }
        return null;
    }
}
