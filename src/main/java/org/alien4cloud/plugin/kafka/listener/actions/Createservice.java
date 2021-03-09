package org.alien4cloud.plugin.kafka.listener.actions;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.plugin.kafka.listener.KafkaConfiguration;
import org.alien4cloud.plugin.kafka.listener.model.Action;
import org.alien4cloud.plugin.kafka.listener.model.Service;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.types.NodeType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

@Slf4j
@Component
public class Createservice extends AbstractAction {

    @Resource
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject 
    private OrchestratorService orchestratorService;
    @Inject
    private LocationService locationService;

    @Inject
    private KafkaConfiguration configuration;

    private String orchestratorId = null;

    @PostConstruct
    public void init() {
       String orchestratorName = configuration.getActionParam ("createservice", "orchestrator");
       if (StringUtils.isBlank(orchestratorName)) {
          log.error("orchestrator not set");
          return;
       }
       Orchestrator orchestrator = null;
       boolean found = false;
       for (Orchestrator orc : orchestratorService.getAllEnabledOrchestrators()) {
          found = orc.getName().equals(orchestratorName);
          if (found) {
             orchestrator = orc;
             break;
          }
       }
       if (!found) {
          log.error ("Can not find orchestrator {}", orchestratorName);
          return;
       }
       orchestratorId = orchestrator.getId();
   }

   private String getLocationId (String locationName) {
       if (orchestratorId == null) {
          log.error ("Orchestrator not set");
          return null;
       }
       boolean found = false;
       for (Location location : locationService.getOrchestratorLocations(orchestratorId)) {
          found = location.getName().equals(locationName);
          if (found) {
             return location.getId();
          }
       }
       if (!found) {
          log.error ("Can not find location {}", locationName);
       }
       return null;
    }

    public Action process (Action action) {

       Action response = initResponse(action);

       /* data is mandatory */
       if (action.getData() == null) {
          log.error ("Request:" + action.getRequestid() + " - No data defined for create service action");
          return completeResponse(response, "KO", "missing data");
       }
       log.debug ("Createservice with data {}", action.getData().toString());
       
       Service service = (new ObjectMapper()).convertValue(action.getData(), Service.class);

       if (service.getNodeInstance() != null) {
            Map<String, AbstractPropertyValue> nodeProperties = service.getNodeInstance().getProperties() == null ? null : new HashMap<String, AbstractPropertyValue>();
            Map<String, Capability> nodeCapabilities = service.getNodeInstance().getCapabilities() == null ? null : new HashMap<String, Capability>();
            Map<String, String> nodeAttributeValues = service.getNodeInstance().getAttributeValues();

            if ((service.getTargets() == null) || (service.getTargets().size() == 0)) {
               log.error ("Targets not set");
               return completeResponse (response, "KO", "missing target");
            }

            String[] locations = safe(service.getTargets()).stream().map(this::getLocationId).filter(e -> e != null).toArray(String[]::new);
            if ((locations.length == 0) || (locations.length != service.getTargets().size())) {
               log.error ("Unknown location(s)");
               return completeResponse (response, "KO", "unknown target");
            }

            String serviceId = serviceResourceService.create(service.getName(), service.getVersion(), service.getNodeType(),
                               service.getNodeTypeVersion());
            log.info ("Service {} ({}) created", service.getName(), service.getNodeType());

            log.debug ("Service attributes: {}", nodeAttributeValues);

            /* build properties */
            safe(service.getNodeInstance().getProperties()).forEach((name, value) -> {
               nodeProperties.put (name, new ScalarPropertyValue(value));
               log.debug ("Service property {} to be set to {}", name, value);
            });

            /* build capabilities properties : look for capability name from node type capabilities definitions */
            NodeType nodeType = toscaTypeSearchService.findOrFail(NodeType.class, service.getNodeType(), service.getNodeTypeVersion());
            safe(service.getNodeInstance().getCapabilities()).forEach((nameC, capa) -> {
               Capability capaObj = getCapability (nameC, nodeType);
               if (capaObj == null) {
                  log.error ("Can not find capability {} for {}", nameC, service.getNodeType());
               } else {
                  nodeCapabilities.put (nameC, capaObj);
                  safe(capa.getProperties()).forEach ((nameP, value)  -> {
                     capaObj.getProperties().put(nameP, new ScalarPropertyValue(value));
                     log.debug ("Service capability {}, property {} to be set to {}", nameC, nameP, value);
                  });
               }
            });
            /* if could not get all capabilities: error... */
            if ((nodeCapabilities != null) && (nodeCapabilities.size() != service.getNodeInstance().getCapabilities().size())) {
               return completeResponse (response, "KO", "unknown capability");
            }

            try {
               serviceResourceService.patch(serviceId, service.getName(), service.getVersion(), null, service.getNodeType(),
                                            service.getNodeTypeVersion(), nodeProperties, nodeCapabilities, nodeAttributeValues, 
                                            locations, null, null);
            } catch (Exception e) {
               log.error ("Can not update service: {}", e.getMessage());
               return completeResponse (response, "KO", e.getMessage());
            }
            log.info ("Service {} ({}) updated", service.getName(), service.getNodeType());
       }

       return completeResponse(response, "OK", null);
    }

    private Capability getCapability (String name, NodeType node) {
       for (CapabilityDefinition capa : node.getCapabilities()) {
          if (capa.getId().equals(name)) {
             return new Capability(capa.getType(), new HashMap<String, AbstractPropertyValue>());
          }
       }
       return null;
    }

}
