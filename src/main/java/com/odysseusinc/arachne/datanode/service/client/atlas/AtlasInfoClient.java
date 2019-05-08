package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.odysseusinc.arachne.datanode.Constants;
import feign.RequestLine;

public interface AtlasInfoClient {
	@RequestLine("GET " + Constants.Atlas.INFO)
	AtlasClient.Info getInfo();
}
