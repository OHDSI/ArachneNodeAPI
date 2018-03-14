package com.odysseusinc.arachne.datanode.dto.atlas;

import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import net.minidev.json.annotate.JsonIgnore;

public class BaseAtlasEntity {

    @JsonIgnore
    protected Atlas origin;

    public Atlas getOrigin() {

        return origin;
    }

    public void setOrigin(Atlas origin) {

        this.origin = origin;
    }
}
