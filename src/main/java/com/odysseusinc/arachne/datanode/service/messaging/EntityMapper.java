package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EntityMapper<L extends BaseAtlasEntity, T, C extends AtlasClient> {

	List<L> getEntityList(C client);

	List<MultipartFile> mapEntity(T entity);
}
