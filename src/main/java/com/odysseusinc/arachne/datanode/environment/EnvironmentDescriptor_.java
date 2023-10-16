package com.odysseusinc.arachne.datanode.environment;

import java.time.Instant;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(EnvironmentDescriptor.class)
public class EnvironmentDescriptor_ {
    public static volatile SingularAttribute<EnvironmentDescriptor, Long> id;
    public static volatile SingularAttribute<EnvironmentDescriptor, String> descriptorId;
    public static volatile SingularAttribute<EnvironmentDescriptor, String> json;
    public static volatile SingularAttribute<EnvironmentDescriptor, String> label;
    public static volatile SingularAttribute<EnvironmentDescriptor, Boolean> base;
    public static volatile SingularAttribute<EnvironmentDescriptor, Instant> terminated;
}
