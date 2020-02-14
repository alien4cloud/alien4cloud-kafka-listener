package org.alien4cloud.plugin.kafka.listener.actions;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.exception.PaaSDeploymentException;
import static alien4cloud.utils.AlienUtils.safe;
import alien4cloud.utils.MapUtil;

import org.alien4cloud.plugin.kafka.listener.KafkaListener;
import org.alien4cloud.plugin.kafka.listener.model.Action;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class Runworkflow extends AbstractAction {

    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private WorkflowExecutionService workflowExecutionService;
    @Inject
    private KafkaListener listener;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    private Application getApplication(String applicationName) {
        Map<String, String[]> filters = MapUtil.newHashMap(new String[] { "name" }, new String[][] { new String[] { applicationName } });
        GetMultipleDataResult<Application> result = alienDAO.search(Application.class, null, filters, 0, 1);
        if (result.getTotalResults() > 0) {
           return result.getData()[0];
        } else {
           return null;
        }
    }

    public Action process (Action action) {
       String applicationId = safe(action.getParameters()).get("appli");
       String environmentName = safe(action.getParameters()).get("env");
       String workflowName = safe(action.getParameters()).get("workflow");

       Action response = initResponse(action);

       /* application name is mandatory */
       if (StringUtils.isBlank(applicationId)) {
          log.error ("Request:" + action.getRequestid() + " - No application defined for run workflow action");
          return completeResponse(response, "KO");
       }

       /* default workflow is "run" */
       if (StringUtils.isBlank(workflowName)) {
          workflowName = "run";
       }

       Application appli = getApplication(applicationId);
       if (appli != null) {
          applicationId = appli.getId();
       } else {
          log.error ("Request:" + action.getRequestid() + " - Application " + applicationId + " not found.");
          return completeResponse(response, "KO");
       }

       /* get application environment id from its name if any */
       ApplicationEnvironment environment = null;
       if (StringUtils.isBlank(environmentName)) {
          environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, null);
       } else {
          ApplicationEnvironment[] envs = applicationEnvironmentService.getByApplicationId (applicationId);
          boolean found = false;
          for (int i = 0 ; (i < envs.length) && !found; i++) {
             found = envs[i].getName().equalsIgnoreCase(environmentName);
             if (found) {
                environment = envs[i];
             }
          }
          if (!found) {
             log.error ("Request:" + action.getRequestid() + " - Environment " + environmentName + " not found for application " + applicationId);
             return completeResponse(response, "KO");
          }
       }

       Map<String, Object> params = new HashMap<String,Object>();
       try {
           log.info ("Request:" + action.getRequestid() + " - Running " + workflowName + " for " + applicationId + "-" + environment.getName());
           // secretProviderConfigurationAndCredentials ???
           workflowExecutionService.launchWorkflow(null, environment.getId(), workflowName, params,
                   new IPaaSCallback<String>() {
                       @Override
                       public void onSuccess(String data) {
                          sendResponse (response, "OK");
                       }

                       @Override
                       public void onFailure(Throwable e) {
                          sendResponse (response, "KO");
                       }
                   });
       } catch (OrchestratorDisabledException e) {
          log.error ("Request:" + action.getRequestid() + " - Error running " + workflowName + " for " + applicationId + "-" + environmentName + " : [OrchestratorDisabledException]" + e.getMessage());
          return completeResponse(response, "KO");
       } catch (PaaSDeploymentException e) {
          log.error ("Request:" + action.getRequestid() + " - Error running " + workflowName + " for " + applicationId + "-" + environmentName + " : [PaaSDeploymentException]" + e.getMessage());
          return completeResponse(response, "KO");
       }
       return null;
    }

    private void sendResponse (Action response,  String status) {
       listener.sendResponse(completeResponse(response, status));
    }

}
