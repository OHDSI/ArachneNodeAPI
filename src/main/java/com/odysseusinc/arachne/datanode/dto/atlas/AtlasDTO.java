package com.odysseusinc.arachne.datanode.dto.atlas;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;

public class AtlasDTO extends AtlasShortDTO {

    private Long id;
    private String url;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }
}
