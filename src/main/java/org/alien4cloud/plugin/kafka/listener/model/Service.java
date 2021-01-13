package org.alien4cloud.plugin.kafka.listener.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Service {
    private String name;

    private String version;

    private String nodeType;

    private String nodeTypeVersion;

    private NodeInstance nodeInstance;
}
