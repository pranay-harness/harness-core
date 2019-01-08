package io.harness.event.usagemetrics;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.event.model.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

@Singleton
public class UsageMetricsService {
  @Inject private AccountService accountService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private TriggerService triggerService;
  @Inject private UsageMetricsEventPublisher publisher;
  @Inject private EnvironmentService environmentService;
  private static final Logger logger = LoggerFactory.getLogger(UsageMetricsService.class);

  public void checkUsageMetrics() {
    getAllAccounts().forEach(account -> {
      try {
        logger.info(
            "Checking Usage metrics for accountId:[{}], accountName:[{}]", account.getUuid(), account.getAccountName());
        List<String> appIds = getAppIds(account.getUuid());
        logger.info("Detected [{}] apps for account [{}]", appIds.size(), account.getAccountName());
        publisher.publishSetupDataMetric(
            account.getUuid(), account.getAccountName(), appIds.size(), EventConstants.NUMBER_OF_APPLICATIONS);
        publisher.publishSetupDataMetric(account.getUuid(), account.getAccountName(),
            getNumberOfServicesPerAccount(appIds), EventConstants.NUMBER_OF_SERVICES);
        publisher.publishSetupDataMetric(account.getUuid(), account.getAccountName(),
            getNumberOfPipelinesPerAccount(appIds), EventConstants.NUMBER_OF_PIPELINES);
        publisher.publishSetupDataMetric(account.getUuid(), account.getAccountName(),
            getNumberOfTriggersPerAccount(appIds), EventConstants.NUMBER_OF_TRIGGERS);
        publisher.publishSetupDataMetric(account.getUuid(), account.getAccountName(),
            getNumberOfEnvironmentsPerAccount(appIds), EventConstants.NUMBER_OF_ENVIRONMENTS);
        publisher.publishSetupDataMetric(account.getUuid(), account.getAccountName(),
            getNumberOfWorkflowsForAccount(account.getUuid()), EventConstants.NUMBER_OF_WORKFLOWS);
      } catch (Exception e) {
        logger.warn("Failed to get Usage metrics for for accountId:[{}], accountName:[{}]", account.getUuid(),
            account.getAccountName(), e);
      }
    });
  }

  protected List<Account> getAllAccounts() {
    return accountService.listAllAccountWithDefaultsWithoutLicenseInfo();
  }

  protected List<String> getAppIds(String accountId) {
    return appService.getAppIdsByAccountId(accountId);
  }

  protected long getNumberOfWorkflowsForAccount(String accountId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    PageRequest<Workflow> pageRequest =
        aPageRequest().addFilter("appId", Operator.IN, appIds.toArray()).addFieldsIncluded("_id").build();
    final PageResponse<Workflow> workflowsList = workflowService.listWorkflows(pageRequest);
    return isEmpty(workflowsList) ? 0 : workflowsList.getTotal();
  }

  protected long getNumberOfServicesPerAccount(List<String> apps) {
    PageRequest<Service> svcPageRequest = aPageRequest()
                                              .withLimit(UNLIMITED)
                                              .addFilter("appId", Operator.IN, apps.toArray())
                                              .addFieldsIncluded(ID_KEY)
                                              .build();
    return serviceResourceService.list(svcPageRequest, false, false).getTotal();
  }

  protected long getNumberOfPipelinesPerAccount(List<String> appIds) {
    PageRequest<Pipeline> pageRequest = aPageRequest()
                                            .withLimit(UNLIMITED)
                                            .addFilter("appId", Operator.IN, appIds.toArray())
                                            .addFieldsIncluded("_id")
                                            .build();
    return pipelineService.listPipelines(pageRequest).getTotal();
  }

  protected long getNumberOfTriggersPerAccount(List<String> appIds) {
    PageRequest<Trigger> pageRequest = aPageRequest()
                                           .withLimit(UNLIMITED)
                                           .addFilter("appId", Operator.IN, appIds.toArray())
                                           .addFieldsIncluded("_id")
                                           .build();
    return triggerService.list(pageRequest).getTotal();
  }

  protected long getNumberOfEnvironmentsPerAccount(List<String> appIds) {
    PageRequest<Environment> pageRequest = aPageRequest()
                                               .withLimit(UNLIMITED)
                                               .addFilter("appId", Operator.IN, appIds.toArray())
                                               .addFieldsIncluded("_id")
                                               .build();
    return environmentService.list(pageRequest, false).getTotal();
  }
}
