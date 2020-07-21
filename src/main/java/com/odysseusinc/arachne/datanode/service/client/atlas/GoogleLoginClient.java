package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.odysseusinc.arachne.datanode.Constants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.ohdsi.authenticator.exception.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginClient {

    public String generateJWTToken(String keyfile) {

        try(InputStream in = new ByteArrayInputStream(keyfile.getBytes())) {
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(in)
                    .createScoped(Collections.singletonList(Constants.GOOGLE_AUTH_SCOPE));
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            return token.getTokenValue();
        } catch (IOException e) {
            throw new AuthenticationException(e.getMessage());
        }
    }

}
