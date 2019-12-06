package org.alien4cloud.plugin.kafka.listener.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Action {

   String action;

   Map<String,String> parameters;

}
