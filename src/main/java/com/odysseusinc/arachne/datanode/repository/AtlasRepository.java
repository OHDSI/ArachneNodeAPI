package com.odysseusinc.arachne.datanode.repository;

import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtlasRepository extends JpaRepository<Atlas, Long> {

    List<Atlas> findByCentralIdIn(List<Long> idList);
}
