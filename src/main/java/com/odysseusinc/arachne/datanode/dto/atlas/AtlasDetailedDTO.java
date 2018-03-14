package com.odysseusinc.arachne.datanode.dto.atlas;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;

public class AtlasDetailedDTO extends AtlasShortDTO {

    private String url;
    private String authType;
    private String username;
    private String password;
    private Boolean cohortLogEnabled;
    private Boolean cohortCountEnabled;

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

    public Boolean getCohortLogEnabled() {

        return cohortLogEnabled;
    }

    public void setCohortLogEnabled(Boolean cohortLogEnabled) {

        this.cohortLogEnabled = cohortLogEnabled;
    }

    public Boolean getCohortCountEnabled() {

        return cohortCountEnabled;
    }

    public void setCohortCountEnabled(Boolean cohortCountEnabled) {

        this.cohortCountEnabled = cohortCountEnabled;
    }
}
