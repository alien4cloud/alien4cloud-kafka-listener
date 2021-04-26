package org.alien4cloud.plugin.kafka.listener.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class NodeInstance {

   private Map<String, String> properties;

   private Map<String, CapabilityProps> capabilities;

   private Map<String, String> attributeValues;

   private MetaProperty[] metaproperties;
}
