package io.harness.event.usagemetrics;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.utils.Validator;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import java.util.List;

/**
 * Created by Pranjal on 01/10/2019
 */
@Singleton
public class UsageMetricsHelper {
  @Inject private WingsPersistence wingsPersistence;

  public Application getApplication(String appId) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .project(ApplicationKeys.accountId, true)
                                  .project(ApplicationKeys.name, true)
                                  .filter(Application.ID_KEY, appId)
                                  .get();
    Validator.notNullCheck("Application does not exist", application, USER);
    return application;
  }

  public String getServiceName(String appId, String serviceId) {
    Service service = wingsPersistence.createQuery(Service.class)
                          .project(Workflow.NAME_KEY, true)
                          .filter(ServiceKeys.appId, appId)
                          .filter(Service.ID_KEY, serviceId)
                          .get();
    Validator.notNullCheck("Service does not exist", service, USER);
    return service.getName();
  }

  public String getEnvironmentName(String appId, String environmentId) {
    Environment environment = wingsPersistence.createQuery(Environment.class)
                                  .project(EnvironmentKeys.name, true)
                                  .filter(EnvironmentKeys.appId, appId)
                                  .filter(Environment.ID_KEY, environmentId)
                                  .get();
    Validator.notNullCheck("Environment does not exist", environment, USER);
    return environment.getName();
  }

  public List<Account> listAllAccountsWithDefaults() {
    PageRequest<Account> pageRequest = aPageRequest()
                                           .addFieldsIncluded(Account.ID_KEY, AccountKeys.accountName)
                                           .addFilter(EnvironmentKeys.appId, Operator.EQ, GLOBAL_APP_ID)
                                           .build();
    return wingsPersistence.getAllEntities(pageRequest, () -> wingsPersistence.query(Account.class, pageRequest));
  }

  public CVConfiguration getCVConfig(String cvConfigId) {
    CVConfiguration cvConfiguration = wingsPersistence.createQuery(CVConfiguration.class)
                                          .project(CVConfigurationKeys.name, true)
                                          .project(CVConfigurationKeys.serviceId, true)
                                          .filter(CVConfiguration.ID_KEY, cvConfigId)
                                          .get();
    Validator.notNullCheck("CV Config does not exist", cvConfiguration, USER);
    return cvConfiguration;
  }

  @Data
  @NoArgsConstructor
  public static class InstanceCount {
    @Id ID id;
    int count;
  }

  @Data
  @NoArgsConstructor
  public static class ID {
    String accountId;
  }
}
