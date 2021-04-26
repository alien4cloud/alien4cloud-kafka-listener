package org.alien4cloud.plugin.kafka.listener.actions;

import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.plugin.kafka.listener.model.Action;
import org.alien4cloud.plugin.kafka.listener.model.Service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

@Slf4j
@Component
public class Checkservice extends AbstractAction {

    @Resource
    private ServiceResourceService serviceResourceService;

    public Action process (Action action) {

       Action response = initResponse(action);

       /* data is mandatory */
       if (action.getData() == null) {
          log.error ("Request:" + action.getRequestid() + " - No data defined for check service action");
          return completeResponse(response, "KO", "missing data");
       }
       log.debug ("Checkservice with data {}", action.getData().toString());
       
       Service service = (new ObjectMapper()).convertValue(action.getData(), Service.class);
       if (StringUtils.isBlank(service.getName()) || StringUtils.isBlank(service.getVersion())) { 
          log.error ("Request:" + action.getRequestid() + " - Invalid data");
          return completeResponse(response, "KO", "invalid data");
       }

       Map<String, String[]> filters = new HashMap<String, String[]>();
       filters.put ("name", new String[]{service.getName()});
       filters.put ("version", new String[]{service.getVersion()});

       if (serviceResourceService.search ("", filters, null, null, true, 0, 1).getTotalResults() == 0) {
          return completeResponse(response, "KO", "Service does not exist");
       } else {
          return completeResponse(response, "OK", "Service exists");
       }

    }
}
