package io.harness.event.usagemetrics;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.UuidAccess.ID_KEY;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.beans.Account.ACCOUNT_NAME_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.persistence.ReadPref;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.utils.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Pranjal on 01/10/2019
 */
@Singleton
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

  public List<Account> listAllAccountsWithDefaults() {
    PageRequest<Account> pageRequest = aPageRequest()
                                           .addFieldsIncluded(ID_KEY, ACCOUNT_NAME_KEY)
                                           .addFilter(APP_ID_KEY, Operator.EQ, GLOBAL_APP_ID)
                                           .build();
    return wingsPersistence.getAllEntities(pageRequest, null);
  }

  public Map<String, Integer> getAllValidInstanceCounts() {
    Map<String, Integer> instanceCountMap = new HashMap<>();
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.criteria("isDeleted").equal(false);
    wingsPersistence.getDatastore(Instance.class, ReadPref.NORMAL)
        .createAggregation(Instance.class)
        .match(query)
        .project(projection("accountId"))
        .group(id(grouping("accountId")), grouping("count", accumulator("$sum", 1)))
        .aggregate(InstanceCount.class)
        .forEachRemaining(
            instanceCount -> instanceCountMap.put(instanceCount.getId().getAccountId(), instanceCount.getCount()));
    return instanceCountMap;
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
