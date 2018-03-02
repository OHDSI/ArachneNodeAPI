package com.odysseusinc.arachne.datanode;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonBuildNumberDTO;

public class BuildNumberDTO extends CommonBuildNumberDTO {

    private String centralUrl;

    public String getCentralUrl() {

        return centralUrl;
    }

    public void setCentralUrl(String centralUrl) {

        this.centralUrl = centralUrl;
    }
}
