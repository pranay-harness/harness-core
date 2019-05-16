package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.licensing.violations.checkers.ApprovalStepViolationChecker.WORKFLOW_APPROVAL_STEP_PREDICATE;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.sm.StateType;
import software.wings.stencils.StencilCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

public interface WorkflowViolationCheckerMixin {
  Predicate<Workflow> WORFKFLOW_TEMPLAE_USAGE_PREDICATE = wf -> isNotEmpty(wf.getLinkedTemplateUuids());

  Predicate<GraphNode> FLOW_CONTROL_STEP_PREDICATE = gn -> {
    StateType stateType = StateType.valueOf(gn.getType());
    return StencilCategory.FLOW_CONTROLS.equals(stateType.getStencilCategory());
  };

  Predicate<GraphNode> WORKFLOW_COMMUNITY_VIOLATION_PREDICATE =
      gn -> FLOW_CONTROL_STEP_PREDICATE.test(gn) || WORKFLOW_APPROVAL_STEP_PREDICATE.test(gn);

  default List<Usage> getWorkflowViolationUsages(
      @NotNull List<Workflow> workflowList, @NotNull Predicate<GraphNode> graphNodePredicate) {
    return workflowList.parallelStream()
        .map(workflow -> {
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          Usage usage = null;
          if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
            CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
                (CanaryOrchestrationWorkflow) orchestrationWorkflow;

            boolean hasViolation =
                checkWorkflowViolation(canaryOrchestrationWorkflow.getWorkflowPhaseIdMap(), graphNodePredicate)
                || checkWorkflowViolation(canaryOrchestrationWorkflow.getPreDeploymentSteps(), graphNodePredicate)
                || checkWorkflowViolation(canaryOrchestrationWorkflow.getPostDeploymentSteps(), graphNodePredicate);

            if (hasViolation) {
              usage = Usage.builder()
                          .entityId(workflow.getUuid())
                          .entityName(workflow.getName())
                          .entityType(EntityType.WORKFLOW.name())
                          .property(Workflow.APP_ID_KEY, workflow.getAppId())
                          .build();
            }
          }
          return usage;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  default boolean checkWorkflowViolation(PhaseStep phaseStep, Predicate<GraphNode> graphNodePredicate) {
    boolean hasViolation = false;
    if (phaseStep != null && phaseStep.getSteps() != null) {
      hasViolation = phaseStep.getSteps()
                         .stream()
                         .filter(gn -> StringUtils.isNotBlank(gn.getType()))
                         .anyMatch(gn -> graphNodePredicate.test(gn));
    }
    return hasViolation;
  }

  default boolean checkWorkflowViolation(
      Map<String, WorkflowPhase> workflowPhaseMap, Predicate<GraphNode> graphNodePredicate) {
    boolean hasViolation = false;
    if (isNotEmpty(workflowPhaseMap)) {
      hasViolation =
          workflowPhaseMap.values()
              .stream()
              .filter(wp -> isNotEmpty(wp.getPhaseSteps()))
              .anyMatch(
                  wp -> wp.getPhaseSteps().stream().anyMatch(ps -> checkWorkflowViolation(ps, graphNodePredicate)));
    }
    return hasViolation;
  }

  default PageRequest<Workflow> getWorkflowsPageRequest(@NotNull String accountId) {
    return (PageRequest<Workflow>) aPageRequest()
        .withLimit(PageRequest.UNLIMITED)
        .addFilter(WorkflowKeys.accountId, Operator.EQ, accountId)
        .build();
  }
}
