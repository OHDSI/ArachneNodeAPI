package com.odysseusinc.arachne.datanode.environment;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "environment_descriptor")
@Getter
@Setter
public class EnvironmentDescriptor {
    @SequenceGenerator(name = "descriptor_id_seq", sequenceName = "descriptor_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "descriptor_id_seq")
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "descriptor_id")
    private String descriptorId;

    @Column(name = "json")
    private String json;

    @Column(name = "label")
    private String label;

    @Column(name = "base")
    private boolean base;

    @Column(name = "terminated")
    private Instant terminated;
}
