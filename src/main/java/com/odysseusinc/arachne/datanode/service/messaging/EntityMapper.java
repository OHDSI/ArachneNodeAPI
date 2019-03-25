package com.odysseusinc.arachne.datanode.service.messaging;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EntityMapper<T> {

	List<MultipartFile> mapEntity(T entity);
}
