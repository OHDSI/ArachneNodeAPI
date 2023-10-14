package com.odysseusinc.arachne.datanode.environment;


import com.odysseusinc.arachne.datanode.service.client.engine.ExecutionEngineClient;
import com.odysseusinc.arachne.datanode.util.JpaSugar;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorsDTO;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EnvironmentDescriptorService {
    volatile boolean firstFetch = true;

    private final Optional<Supplier<RuntimeEnvironmentDescriptorsDTO>> fetchDescriptors;

    @PersistenceContext
    private EntityManager em;

    public EnvironmentDescriptorService(ExecutionEngineClient client) {
        this.fetchDescriptors = client.getDescriptors();
    }

    @Scheduled(fixedDelayString = "${execution.descriptors.delayMs:60000}")
    @Transactional
    public void updateDescriptors() {
        fetchDescriptors.ifPresent(supplier -> {
            RuntimeEnvironmentDescriptorsDTO descriptorsDTO = supplier.get();
            Map<String, EnvironmentDescriptor> index = getAll().collect(
                    Collectors.toMap(EnvironmentDescriptor::getDescriptorId, Function.identity())
            );

            List<String> updated = descriptorsDTO.getDescriptors().stream().map(dto ->
                    Optional.ofNullable(index.get(dto.getId())).map(updateFrom(dto)).orElseGet(create(dto))
            ).map(EnvironmentDescriptor::getDescriptorId).collect(Collectors.toList());
            log.debug("Processed {} descriptors", updated.size());
            index.forEach((id, desc) -> {
                if (!updated.contains(id)) {
                    log.info("Removed descriptor [{}]", id);
                    em.remove(desc);
                }
            });
            if (firstFetch) {
                firstFetch = false;
                getAll().forEach(dto ->
                        log.info("Validated descriptor #{} [{}] - [{}], {} bytes", dto.getId(), dto.getDescriptorId(), dto.getLabel(), dto.getJson().length())
                );
            }
        });
    }

    private Stream<EnvironmentDescriptor> getAll() {
        return JpaSugar.selectAll(em, EnvironmentDescriptor.class).getResultStream();
    }

    private Supplier<EnvironmentDescriptor> create(RuntimeEnvironmentDescriptorDTO dto) {
        return () -> {
            log.info("Created descriptor [{}] - [{}], bundle name [{}]", dto.getId(), dto.getLabel(), dto.getBundleName());
            EnvironmentDescriptor entity = updateFrom(dto).apply(new EnvironmentDescriptor());
            em.persist(entity);
            return entity;
        };
    }

    private UnaryOperator<EnvironmentDescriptor> updateFrom(RuntimeEnvironmentDescriptorDTO dto) {
        return entity -> {
            entity.setDescriptorId(dto.getId());
            entity.setBase(dto.getId().toLowerCase().startsWith("default"));
            entity.setLabel(dto.getLabel());
            entity.setJson(SerializationUtils.serialize(dto));
            return entity;
        };
    }


}