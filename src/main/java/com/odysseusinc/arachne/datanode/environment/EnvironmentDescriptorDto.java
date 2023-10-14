package com.odysseusinc.arachne.datanode.environment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class EnvironmentDescriptorDto {
    private final Long id;
    private final String descriptorId;
    private final String label;
    private final String json;
}
