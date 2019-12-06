package org.alien4cloud.plugin.kafka.listener.actions;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.exception.PaaSDeploymentException;
import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.plugin.kafka.listener.model.Action;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class Runworkflow implements IAction {

    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private WorkflowExecutionService workflowExecutionService;

    private ApplicationEnvironment getAppEnvironment(String applicationId, String applicationEnvironmentId) {
        Application application = applicationService.getOrFail(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        return environment;
    }

    public void process (Action action) {
       String applicationId = safe(action.getParameters()).get("appli");
       String environmentName = safe(action.getParameters()).get("env");
       String workflowName = safe(action.getParameters()).get("workflow");

       /* application name is mandatory */
       if (StringUtils.isBlank(applicationId)) {
          log.error ("No application defined for run workflow action");
          return;
       }

       /* default workflow is "run" */
       if (StringUtils.isBlank(workflowName)) {
          workflowName = "run";
       }

       /* get application environment id from its name if any */
       String applicationEnvironmentId = null;
       if (StringUtils.isBlank(environmentName)) {
          applicationEnvironmentId = null;
       } else {
          ApplicationEnvironment[] envs = applicationEnvironmentService.getByApplicationId (applicationId);
          boolean found = false;
          for (int i = 0 ; (i < envs.length) && !found; i++) {
             found = envs[i].getName().equalsIgnoreCase(environmentName);
             if (found) {
                applicationEnvironmentId = envs[i].getId();
             }
          }
          if (!found) {
             log.error ("Environment " + environmentName + " not found for application " + applicationId);
             return;
          }
       }

       ApplicationEnvironment environment = getAppEnvironment(applicationId, applicationEnvironmentId);
       Map<String, Object> params = new HashMap<String,Object>();

       try {
           log.info ("Running " + workflowName + " for " + applicationId + "-" + environment.getName());
           // secretProviderConfigurationAndCredentials ???
           workflowExecutionService.launchWorkflow(null, environment.getId(), workflowName, params,
                   new IPaaSCallback<String>() {
                       @Override
                       public void onSuccess(String data) {
                          // what should we do ?
                       }

                       @Override
                       public void onFailure(Throwable e) {
                          // what should we do ?
                       }
                   });
       } catch (OrchestratorDisabledException e) {
          log.error ("Error running " + workflowName + " for " + applicationId + "-" + environmentName + " : [OrchestratorDisabledException]" + e.getMessage());
       } catch (PaaSDeploymentException e) {
          log.error ("Error running " + workflowName + " for " + applicationId + "-" + environmentName + " : [PaaSDeploymentException]" + e.getMessage());
       }
    }

}
