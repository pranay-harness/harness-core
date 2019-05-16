package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;

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
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;

public abstract class AbstractWorkflowViolationChecker implements FeatureViolationChecker {
  private WorkflowService workflowService;

  public AbstractWorkflowViolationChecker(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  protected List<Usage> getWorkflowViolationUsages(String accountId, Predicate<GraphNode> graphNodePredicate) {
    List<Usage> violationUsages = Lists.newArrayList();

    getAllWorkflowsByAccountId(accountId).forEach(workflow -> {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;

        boolean hasViolation =
            checkWorkflowViolation(canaryOrchestrationWorkflow.getWorkflowPhaseIdMap(), graphNodePredicate)
            || checkWorkflowViolation(canaryOrchestrationWorkflow.getPreDeploymentSteps(), graphNodePredicate)
            || checkWorkflowViolation(canaryOrchestrationWorkflow.getPostDeploymentSteps(), graphNodePredicate);

        if (hasViolation) {
          violationUsages.add(Usage.builder()
                                  .entityId(workflow.getUuid())
                                  .entityName(workflow.getName())
                                  .entityType(EntityType.WORKFLOW.name())
                                  .build());
        }
      }
    });
    return violationUsages;
  }

  protected List<Workflow> getAllWorkflowsByAccountId(@NotNull String accountId) {
    PageRequest<Workflow> pageRequest = aPageRequest()
                                            .withLimit(PageRequest.UNLIMITED)
                                            .addFilter(WorkflowKeys.accountId, Operator.EQ, accountId)
                                            .build();
    return workflowService.listWorkflows(pageRequest).getResponse();
  }

  private boolean checkWorkflowViolation(PhaseStep phaseStep, Predicate<GraphNode> graphNodePredicate) {
    boolean hasViolation = false;
    if (phaseStep != null && phaseStep.getSteps() != null) {
      hasViolation = phaseStep.getSteps()
                         .stream()
                         .filter(gn -> StringUtils.isNotBlank(gn.getType()))
                         .anyMatch(gn -> graphNodePredicate.test(gn));
    }
    return hasViolation;
  }

  private boolean checkWorkflowViolation(
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
}
