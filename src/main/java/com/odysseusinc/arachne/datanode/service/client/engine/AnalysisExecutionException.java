package com.odysseusinc.arachne.datanode.service.client.engine;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class AnalysisExecutionException extends RuntimeException {

    public AnalysisExecutionException(String message) {
        super(message);
    }

    public AnalysisExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}