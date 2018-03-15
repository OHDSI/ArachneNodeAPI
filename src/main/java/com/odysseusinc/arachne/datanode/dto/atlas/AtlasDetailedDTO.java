package com.odysseusinc.arachne.datanode.dto.atlas;

public class AtlasDetailedDTO extends AtlasDTO {

    private String url;
    private String authType;
    private String username;
    private String password;

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }

    public String getAuthType() {

        return authType;
    }

    public void setAuthType(String authType) {

        this.authType = authType;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }
}
