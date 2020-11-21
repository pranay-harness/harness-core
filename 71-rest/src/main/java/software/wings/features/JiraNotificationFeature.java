package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.features.utils.WorkflowUtils.JIRA_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.getMatchingWorkflows;

import io.harness.annotations.dev.OwnedBy;

import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class JiraNotificationFeature extends AbstractNotificationFeature {
  public static final String FEATURE_NAME = "JIRA_NOTIFICATION";

  @Inject
  public JiraNotificationFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, WorkflowService workflowService) {
    super(accountService, featureRestrictions, workflowService);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  // gets usages of JIRA state under workflows
  @Override
  protected List<Usage> getUsages(String accountId) {
    return getMatchingWorkflows(getAllWorkflowsByAccountId(accountId), JIRA_USAGE_PREDICATE)
        .stream()
        .map(WorkflowUtils::toUsage)
        .collect(Collectors.toList());
  }
}
