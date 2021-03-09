package org.alien4cloud.plugin.kafka.listener.actions;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.common.Usage;
import alien4cloud.model.service.ServiceResource;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.alm.service.exceptions.ServiceUsageException;
import org.alien4cloud.plugin.kafka.listener.model.Action;
import org.alien4cloud.plugin.kafka.listener.model.Service;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

@Slf4j
@Component
public class Deleteservice extends AbstractAction {

    @Resource
    private ServiceResourceService serviceResourceService;

    public Action process (Action action) {

       Action response = initResponse(action);

       /* data is mandatory */
       if (action.getData() == null) {
          log.error ("Request:" + action.getRequestid() + " - No data defined for delete service action");
          return completeResponse(response, "KO", "missing data");
       }
       log.debug ("Deleteteservice with data {}", action.getData().toString());
       
       Service service = (new ObjectMapper()).convertValue(action.getData(), Service.class);

       Map<String, String[]> filters = new HashMap<String, String[]>();
       if (StringUtils.isBlank(service.getName())) {
          log.error("Missing service name");
          return completeResponse(response, "KO", "missing service name");
       }
       if (StringUtils.isBlank(service.getVersion())) {
          log.error("Missing service version");
          return completeResponse(response, "KO", "missing service version");
       }

       String[] name = new String[1];
       name[0] = service.getName();
       filters.put ("name", name);
       String[] version = new String[1];
       version[0] = service.getVersion();
       filters.put ("version", version);
       FacetedSearchResult<ServiceResource> result = serviceResourceService.search ("", filters, null, null, true, 0, 10000);
       if ((result == null) ||
           (result.getData() == null) ||
           (result.getData().length != 1)) {
          log.error ("Cannot find service {} with version {}", service.getName(), service.getVersion());
          return completeResponse(response, "KO", "unknown service");
       }

       try {
          serviceResourceService.delete (result.getData()[0].getId());
       } catch (ServiceUsageException e) {
          StringBuffer resp = new StringBuffer(e.getMessage());
          for (Usage u : e.getUsages()) {
             resp.append(" ").append(u.getResourceName());
          }
          log.error(resp.toString());
          return completeResponse(response, "KO", resp.toString());
       }

       return completeResponse(response, "OK", null);
    }

}
