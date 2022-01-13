/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.utils;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.USER_GROUP;
import static software.wings.sm.states.ApprovalState.USER_GROUPS_VARIABLE;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.features.ApprovalFlowFeature;
import software.wings.features.api.Restrictions;
import software.wings.features.api.Usage;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.stencils.StencilCategory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class WorkflowUtils {
  private WorkflowUtils() {
    throw new AssertionError();
  }

  public static final Predicate<GraphNode> JIRA_USAGE_PREDICATE =
      gn -> Objects.equals(StateType.JIRA_CREATE_UPDATE.name(), gn.getType());

  public static final Predicate<GraphNode> SERVICENOW_USAGE_PREDICATE =
      gn -> Objects.equals(StateType.SERVICENOW_CREATE_UPDATE.name(), gn.getType());

  public static final Predicate<Workflow> TEMPLATE_USAGE_PREDICATE = wf -> isNotEmpty(wf.getLinkedTemplateUuids());

  public static final Predicate<GraphNode> FLOW_CONTROL_USAGE_PREDICATE = gn
      -> Arrays.stream(StateType.values())
             .filter(stateType -> stateType.getStencilCategory() == StencilCategory.FLOW_CONTROLS)
             .map(Enum::name)
             .anyMatch(stateType -> Objects.equals(stateType, gn.getType()));

  public static List<Workflow> getWorkflowsWithTemplateLibrary(List<Workflow> workflows) {
    return workflows.stream().filter(TEMPLATE_USAGE_PREDICATE).collect(Collectors.toList());
  }

  public static Set<ApprovalStateType> toDisallowedApprovalSteps(Restrictions restrictions) {
    @SuppressWarnings("unchecked")
    Set<ApprovalStateType> allowedApprovalStepTypes =
        ((List<String>) restrictions.getOrDefault(
             ApprovalFlowFeature.ALLOWED_APPROVAL_STEPS_KEY, Collections.emptyList()))
            .stream()
            .map(ApprovalStateType::valueOf)
            .collect(toSet());

    Set<ApprovalStateType> disallowedApprovalStepTypes = EnumSet.allOf(ApprovalStateType.class);
    disallowedApprovalStepTypes.removeAll(allowedApprovalStepTypes);

    return disallowedApprovalStepTypes;
  }

  public static boolean hasApprovalSteps(GraphNode gn, Collection<ApprovalStateType> approvalStepTypes) {
    return hasApprovalSteps(gn.getType(), gn.getProperties(), approvalStepTypes);
  }

  public static boolean hasApprovalSteps(PipelineStageElement pse, Collection<ApprovalStateType> approvalStepTypes) {
    return hasApprovalSteps(pse.getType(), pse.getProperties(), approvalStepTypes);
  }

  private static boolean hasApprovalSteps(
      String type, Map<String, Object> props, Collection<ApprovalStateType> approvalStepTypes) {
    if (!StateType.APPROVAL.name().equals(type) && isEmpty(props)) {
      return false;
    }

    for (ApprovalStateType approvalStepType : approvalStepTypes) {
      if (approvalStepType == USER_GROUP && props.containsKey(USER_GROUPS_VARIABLE)) {
        return true;
      }

      if (props.containsKey(APPROVAL_STATE_TYPE_VARIABLE)
          && (approvalStepType == props.get(APPROVAL_STATE_TYPE_VARIABLE)
              || approvalStepType.name().equals(props.get(APPROVAL_STATE_TYPE_VARIABLE)))) {
        return true;
      }
    }

    return false;
  }

  public static List<Workflow> getMatchingWorkflows(Collection<Workflow> workflows, Predicate<GraphNode> predicate) {
    return workflows.stream().filter(workflow -> matches(workflow, predicate)).collect(toList());
  }

  public static boolean matches(Workflow workflow, Predicate<GraphNode> predicate) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;

      return checkWorkflowViolation(canaryOrchestrationWorkflow.getWorkflowPhaseIdMap(), predicate)
          || checkWorkflowViolation(canaryOrchestrationWorkflow.getPreDeploymentSteps(), predicate)
          || checkWorkflowViolation(canaryOrchestrationWorkflow.getPostDeploymentSteps(), predicate);
    }

    return false;
  }

  private static boolean checkWorkflowViolation(PhaseStep phaseStep, Predicate<GraphNode> graphNodePredicate) {
    boolean hasViolation = false;
    if (phaseStep != null && phaseStep.getSteps() != null) {
      hasViolation =
          phaseStep.getSteps().stream().filter(gn -> StringUtils.isNotBlank(gn.getType())).anyMatch(graphNodePredicate);
    }
    return hasViolation;
  }

  private static boolean checkWorkflowViolation(
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

  public static PageRequest<Workflow> getWorkflowsPageRequest(String accountId) {
    return (PageRequest<Workflow>) aPageRequest()
        .withLimit(PageRequest.UNLIMITED)
        .addFilter(WorkflowKeys.accountId, Operator.EQ, accountId)
        .build();
  }

  public static Usage toUsage(Workflow workflow) {
    return Usage.builder()
        .entityId(workflow.getUuid())
        .entityName(workflow.getName())
        .entityType(EntityType.WORKFLOW.name())
        .property(WorkflowKeys.appId, workflow.getAppId())
        .build();
  }

  public static List<Workflow> getWorkflowsWithApprovalSteps(
      Collection<Workflow> workflows, Set<ApprovalStateType> approvalStepTypes) {
    return getMatchingWorkflows(workflows, pse -> hasApprovalSteps(pse, approvalStepTypes));
  }
}
