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
import software.wings.beans.GraphNode;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.stencils.StencilCategory;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Singleton
@Slf4j
public class FlowControlViolationChecker extends AbstractWorkflowViolationChecker {
  private Predicate<GraphNode> flowControlPredicate;

  @Inject
  public FlowControlViolationChecker(WorkflowService workflowService) {
    super(workflowService);
    flowControlPredicate = gn -> {
      StateType stateType = StateType.valueOf(gn.getType());
      return StencilCategory.FLOW_CONTROLS.equals(stateType.getStencilCategory());
    };
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info(
        "Checking Flow control violations for accountId={} and targetAccountType={}", accountId, AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    List<Usage> flowControlUsages = getWorkflowViolationUsages(accountId, flowControlPredicate);
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
}
