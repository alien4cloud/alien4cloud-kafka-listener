package org.alien4cloud.plugin.kafka.listener.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Action {

   String requestid;

   String datetime;

   String action;

   Map<String,String> parameters;

   JsonNode data;
}
