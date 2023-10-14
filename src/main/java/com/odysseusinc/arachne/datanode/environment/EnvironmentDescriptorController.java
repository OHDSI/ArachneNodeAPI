package com.odysseusinc.arachne.datanode.environment;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/descriptor")
public class EnvironmentDescriptorController {
    @Autowired
    private EnvironmentDescriptorService descriptorService;

    @GetMapping
    public List<EnvironmentDescriptorDto> list() {
        return descriptorService.listActive();
    }
}
