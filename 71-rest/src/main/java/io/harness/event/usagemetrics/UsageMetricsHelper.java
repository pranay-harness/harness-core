package io.harness.event.usagemetrics;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Base.APP_ID_KEY;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.utils.Validator;

/**
 * Created by Pranjal on 01/10/2019
 */
public class UsageMetricsHelper {
  @Inject private WingsPersistence wingsPersistence;

  public Application getApplication(String appId) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .project(Application.ACCOUNT_ID_KEY, true)
                                  .project(Application.NAME_KEY, true)
                                  .filter(Application.ID_KEY, appId)
                                  .get();
    Validator.notNullCheck("Application does not exist", application, USER);
    return application;
  }

  public String getServiceName(String appId, String serviceId) {
    Service service = wingsPersistence.createQuery(Service.class)
                          .project(Workflow.NAME_KEY, true)
                          .filter(APP_ID_KEY, appId)
                          .filter(Service.ID_KEY, serviceId)
                          .get();
    Validator.notNullCheck("Service does not exist", service, USER);
    return service.getName();
  }

  public String getEnvironmentName(String appId, String environmentId) {
    Environment environment = wingsPersistence.createQuery(Environment.class)
                                  .project(Environment.NAME_KEY, true)
                                  .filter(APP_ID_KEY, appId)
                                  .filter(Environment.ID_KEY, environmentId)
                                  .get();
    Validator.notNullCheck("Environment does not exist", environment, USER);
    return environment.getName();
  }

  public String getWorkFlowName(String appId, String workflowId) {
    Workflow workflow = wingsPersistence.createQuery(Workflow.class)
                            .project(Workflow.NAME_KEY, true)
                            .filter(APP_ID_KEY, appId)
                            .filter(Pipeline.ID_KEY, workflowId)
                            .get();
    Validator.notNullCheck("Workflow does not exist", workflow, USER);
    return workflow.getName();
  }
}
