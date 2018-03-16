package com.odysseusinc.arachne.datanode.dto.atlas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;

public class BaseAtlasEntity {

    @JsonIgnore
    protected Atlas origin;

    protected String name;

    public Atlas getOrigin() {

        return origin;
    }

    public void setOrigin(Atlas origin) {

        this.origin = origin;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }
}
