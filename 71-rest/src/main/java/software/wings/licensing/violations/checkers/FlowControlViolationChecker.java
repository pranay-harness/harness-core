package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AccountType;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.stencils.StencilCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Singleton
@Slf4j
public class FlowControlViolationChecker implements FeatureViolationChecker {
  private Predicate<String> flowControlPredicate;
  private WorkflowService workflowService;

  @Inject
  public FlowControlViolationChecker(WorkflowService workflowService) {
    this.workflowService = workflowService;
    flowControlPredicate = s -> {
      StateType stateType = StateType.valueOf(s);
      return StencilCategory.FLOW_CONTROLS.equals(stateType.getStencilCategory());
    };
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info(
        "Checking Flow control violations for accountId={} and targetAccountType={}", accountId, AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    List<Usage> flowControlUsages = getFlowControlViolationUsages(accountId);
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

  private List<Usage> getFlowControlViolationUsages(String accountId) {
    List<Usage> flowControlUsages = Lists.newArrayList();

    getAllWorkflowsByAccountId(accountId).forEach(workflow -> {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;

        boolean hasFlowControlViolation = checkFlowControlViolation(canaryOrchestrationWorkflow.getWorkflowPhaseIdMap())
            || checkFlowControlViolation(canaryOrchestrationWorkflow.getPreDeploymentSteps())
            || checkFlowControlViolation(canaryOrchestrationWorkflow.getPostDeploymentSteps());

        if (hasFlowControlViolation) {
          flowControlUsages.add(Usage.builder()
                                    .entityId(workflow.getUuid())
                                    .entityName(workflow.getName())
                                    .entityType(EntityType.WORKFLOW.name())
                                    .build());
        }
      }
    });
    return flowControlUsages;
  }

  private List<Workflow> getAllWorkflowsByAccountId(String accountId) {
    PageRequest<Workflow> pageRequest = aPageRequest()
                                            .withLimit(PageRequest.UNLIMITED)
                                            .addFilter(WorkflowKeys.accountId, Operator.EQ, accountId)
                                            .build();
    return workflowService.listWorkflows(pageRequest).getResponse();
  }

  private boolean checkFlowControlViolation(PhaseStep phaseStep) {
    boolean hasFlowControl = false;
    if (phaseStep != null && phaseStep.getSteps() != null) {
      hasFlowControl = phaseStep.getSteps()
                           .stream()
                           .filter(gn -> StringUtils.isNotBlank(gn.getType()))
                           .anyMatch(gn -> flowControlPredicate.test(gn.getType()));
    }
    return hasFlowControl;
  }

  private boolean checkFlowControlViolation(Map<String, WorkflowPhase> workflowPhaseMap) {
    boolean hasFlowControl = false;
    if (isNotEmpty(workflowPhaseMap)) {
      hasFlowControl = workflowPhaseMap.values()
                           .stream()
                           .filter(wp -> isNotEmpty(wp.getPhaseSteps()))
                           .anyMatch(wp -> wp.getPhaseSteps().stream().anyMatch(ps -> checkFlowControlViolation(ps)));
    }
    return hasFlowControl;
  }
}
