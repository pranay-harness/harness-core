package software.wings.features;

import static software.wings.features.utils.WorkflowUtils.SERVICENOW_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.getMatchingWorkflows;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ServiceNowNotificationFeature extends AbstractNotificationFeature {
  public static final String FEATURE_NAME = "SERVICENOW_NOTIFICATION";

  @Inject
  public ServiceNowNotificationFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, WorkflowService workflowService) {
    super(accountService, featureRestrictions, workflowService);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  // gets usages of Service Now state under workflows
  @Override
  protected List<Usage> getUsages(String accountId) {
    return getMatchingWorkflows(getAllWorkflowsByAccountId(accountId), SERVICENOW_USAGE_PREDICATE)
        .stream()
        .map(WorkflowUtils::toUsage)
        .collect(Collectors.toList());
  }
}
