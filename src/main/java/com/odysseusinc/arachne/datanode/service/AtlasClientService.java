package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasInfoClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasLoginClient;
import feign.Client;

public interface AtlasClientService {

    <T extends AtlasClient> T buildAtlasClient(Atlas atlas);

    AtlasInfoClient buildAtlasInfoClient(Atlas atlas);

    AtlasLoginClient buildAtlasLoginClient(String url, Client httpClient);
}
