package com.odysseusinc.arachne.datanode.service.client.decoders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.ValidationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;

public class ErrorResultDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {

        if (response.status() == 200 && response.body().length() > 0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonResult result = mapper.readValue(response.body().asReader(), JsonResult.class);
                switch (result.getErrorCode()) {
                    case 3: //ValidationError
                        throw new ValidationException(response.reason());
                    case 2: //Not authorized
                        throw new AuthException(response.reason());
                    default: //System runtime
                        throw new ArachneSystemRuntimeException(response.reason());
                }
            }catch (IOException ignored){
            }
        }
        return null;
    }
}
