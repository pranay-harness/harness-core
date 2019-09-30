package io.harness.event.usagemetrics;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.event.model.EventConstants.ACCOUNT_ID;
import static io.harness.event.model.EventConstants.IS_24X7_ENABLED;
import static io.harness.event.model.EventConstants.VERIFICATION_STATE_TYPE;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.metrics.HarnessMetricRegistry;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.common.VerificationConstants;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class UsageMetricsService {
  @Inject private UsageMetricsHelper usageMetricsHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private TriggerService triggerService;
  @Inject private EnvironmentService environmentService;
  @Inject private AccountService accountService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private HarnessMetricRegistry harnessMetricRegistry;

  public void checkUsageMetrics() {
    Map<String, List<CVConfiguration>> accountIdConfigList = new HashMap<>();
    accountService.listAllAccounts()
        .stream()
        .filter(account -> !account.getUuid().equals(GLOBAL_ACCOUNT_ID))
        .filter(account
            -> account.getLicenseInfo() == null
                || ((account.getLicenseInfo() != null && account.getLicenseInfo().getAccountStatus() != null)
                       && (account.getLicenseInfo().getAccountStatus().equals(AccountStatus.ACTIVE)
                              && (account.getLicenseInfo().getAccountType().equals(AccountType.TRIAL)
                                     || account.getLicenseInfo().getAccountType().equals(AccountType.PAID)))))
        .forEach(account -> {
          try {
            logger.info("Checking Usage metrics for accountId:[{}], accountName:[{}]", account.getUuid(),
                account.getAccountName());

            List<String> appIds = getAppIds(account.getUuid());
            logger.info("Detected [{}] apps for account [{}]", appIds.size(), account.getAccountName());

            List<CVConfiguration> cvConfigurationList = cvConfigurationService.listConfigurations(account.getUuid());
            if (isNotEmpty(cvConfigurationList)) {
              accountIdConfigList.put(account.getUuid(), cvConfigurationList);
            }
          } catch (Exception e) {
            logger.warn("Failed to get Usage metrics for for accountId:[{}], accountName:[{}]", account.getUuid(),
                account.getAccountName(), e);
          }
        });
    createVerificationUsageEvents(accountIdConfigList);
  }

  private void createVerificationUsageEvents(Map<String, List<CVConfiguration>> accountIdConfigList) {
    List<StateType> stateTypes = VerificationConstants.getAnalysisStates();
    accountIdConfigList.forEach((accountId, configList) -> {
      for (StateType stateType : stateTypes) {
        Map properties = new HashMap();
        properties.put(ACCOUNT_ID, accountId);
        properties.put(VERIFICATION_STATE_TYPE, stateType.name());
        properties.put(IS_24X7_ENABLED, true);
        int count = configList.stream()
                        .filter(cvConfiguration
                            -> cvConfiguration.isEnabled24x7()
                                && cvConfiguration.getStateType().name().equals(stateType.name()))
                        .collect(Collectors.toList())
                        .size();
        if (count > 0) {
          emitCVUsageMetrics(properties, count);
        }
      }
    });
  }

  private void emitCVUsageMetrics(Map<String, String> properties, int metricValue) {
    Preconditions.checkNotNull(properties.get(ACCOUNT_ID));
    Preconditions.checkNotNull(properties.get(VERIFICATION_STATE_TYPE));
    Preconditions.checkNotNull(properties.get(IS_24X7_ENABLED));

    harnessMetricRegistry.recordGaugeValue(VerificationConstants.CV_META_DATA,
        new String[] {properties.get(ACCOUNT_ID), properties.get(VERIFICATION_STATE_TYPE),
            String.valueOf(properties.get(IS_24X7_ENABLED))},
        metricValue);
  }

  protected List<Account> getAllAccounts() {
    return usageMetricsHelper.listAllAccountsWithDefaults();
  }

  protected List<String> getAppIds(String accountId) {
    return appService.getAppIdsByAccountId(accountId);
  }

  protected long getNumberOfWorkflowsForAccount(String accountId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    if (appIds.size() > 0) {
      PageRequest<Workflow> pageRequest =
          aPageRequest().addFilter("appId", Operator.IN, appIds.toArray()).addFieldsIncluded("_id").build();
      final PageResponse<Workflow> workflowsList = workflowService.listWorkflows(pageRequest);
      return isEmpty(workflowsList) ? 0 : workflowsList.getTotal();
    }
    return 0;
  }

  protected long getNumberOfServicesPerAccount(List<String> apps) {
    if (apps.size() > 0) {
      PageRequest<Service> svcPageRequest = aPageRequest()
                                                .withLimit(UNLIMITED)
                                                .addFilter("appId", Operator.IN, apps.toArray())
                                                .addFieldsIncluded(ID_KEY)
                                                .build();
      return serviceResourceService.list(svcPageRequest, false, false, false, null).getTotal();
    } else {
      return 0;
    }
  }

  protected long getNumberOfPipelinesPerAccount(List<String> appIds) {
    if (appIds.size() > 0) {
      PageRequest<Pipeline> pageRequest = aPageRequest()
                                              .withLimit(UNLIMITED)
                                              .addFilter("appId", Operator.IN, appIds.toArray())
                                              .addFieldsIncluded("_id")
                                              .build();
      return pipelineService.listPipelines(pageRequest).getTotal();
    } else {
      return 0;
    }
  }

  protected long getNumberOfTriggersPerAccount(List<String> appIds) {
    if (appIds.size() > 0) {
      PageRequest<Trigger> pageRequest = aPageRequest()
                                             .withLimit(UNLIMITED)
                                             .addFilter("appId", Operator.IN, appIds.toArray())
                                             .addFieldsIncluded("_id")
                                             .build();
      return triggerService.list(pageRequest, false, null).getTotal();
    } else {
      return 0;
    }
  }

  protected long getNumberOfEnvironmentsPerAccount(List<String> appIds) {
    if (appIds.size() > 0) {
      PageRequest<Environment> pageRequest = aPageRequest()
                                                 .withLimit(UNLIMITED)
                                                 .addFilter("appId", Operator.IN, appIds.toArray())
                                                 .addFieldsIncluded("_id")
                                                 .build();
      return environmentService.list(pageRequest, false, false, null).getTotal();
    } else {
      return 0;
    }
  }
}
