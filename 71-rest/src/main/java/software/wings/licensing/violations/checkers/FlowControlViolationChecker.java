package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.Workflow;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class FlowControlViolationChecker implements FeatureViolationChecker, WorkflowViolationCheckerMixin {
  private WorkflowService workflowService;

  @Inject
  public FlowControlViolationChecker(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info(
        "Checking Flow control violations for accountId={} and targetAccountType={}", accountId, AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    List<Usage> flowControlUsages =
        getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), FLOW_CONTROL_STEP_PREDICATE);
    if (isNotEmpty(flowControlUsages)) {
      logger.info("Found {} Flow control violations for accountId={} and targetAccountType={}",
          flowControlUsages.size(), accountId, AccountType.COMMUNITY);
      featureViolationList = Collections.singletonList(FeatureUsageViolation.builder()
                                                           .restrictedFeature(RestrictedFeature.FLOW_CONTROL)
                                                           .usages(flowControlUsages)
                                                           .build());
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  private List<Workflow> getAllWorkflowsByAccountId(@NotNull String accountId) {
    return workflowService.listWorkflows(getWorkflowsPageRequest(accountId)).getResponse();
  }
}
