/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.queuing;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.fetchIndex;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.common.InfrastructureConstants.QUEUING_RC_NAME;
import static software.wings.common.InfrastructureConstants.RC_INFRA_STEP_NAME;
import static software.wings.common.InfrastructureConstants.STATE_TIMEOUT_KEY_NAME;
import static software.wings.common.InfrastructureConstants.WEEK_TIMEOUT;
import static software.wings.sm.StateType.ARM_CREATE_RESOURCE;
import static software.wings.sm.StateType.CLOUD_FORMATION_CREATE_STACK;
import static software.wings.sm.StateType.RESOURCE_CONSTRAINT;
import static software.wings.sm.StateType.SHELL_SCRIPT_PROVISION;
import static software.wings.sm.StateType.TERRAFORM_PROVISION;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ResourceConstraint;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.sm.states.ResourceConstraintState.AcquireMode;
import software.wings.sm.states.ResourceConstraintState.ResourceConstraintStateKeys;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowConcurrencyHelper {
  @Inject private AppService appService;
  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  private static final List<String> PROVISIONER_STEP_TYPES = Arrays.asList(CLOUD_FORMATION_CREATE_STACK.name(),
      TERRAFORM_PROVISION.name(), SHELL_SCRIPT_PROVISION.name(), ARM_CREATE_RESOURCE.name());

  public OrchestrationWorkflow enhanceWithConcurrencySteps(Workflow workflow, Map<String, String> workflowVariables) {
    try {
      String appId = workflow.getAppId();
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
      if (!concurrencyEnabled(canaryOrchestrationWorkflow)) {
        return canaryOrchestrationWorkflow;
      }

      boolean resourceConstraintAdded = false;
      if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
        resourceConstraintAdded =
            addResourceConstraintForConcurrency(appId, canaryOrchestrationWorkflow, workflowVariables);
      }
      if (resourceConstraintAdded) {
        canaryOrchestrationWorkflow.setGraph(canaryOrchestrationWorkflow.generateGraph());
      }
      return canaryOrchestrationWorkflow;
    } catch (Exception e) {
      log.error("Failed to enhance workflow with concurrency steps: {}", e.getMessage());
      return workflow.getOrchestrationWorkflow();
    }
  }

  /**
   * This method checks if infra is dynamically provisioned. If not adds a Resource Constraint Step as first
   * If dynamically provisioned adds a Resource Constraint step after the PROVISION_INFRASTRUCTURE step
   *
   * @param appId AppId for the workflow
   * @param canaryOrchestrationWorkflow WorkflowPhase
   * @return WorkflowPhase After adding the Resource Constraint Step If required
   */
  private boolean addResourceConstraintForConcurrency(
      String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow, Map<String, String> workflowVariables) {
    boolean resourceConstraintAdded = false;
    List<String> expressionsHandled = new ArrayList<>();
    TemplateExpression expression;
    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      String infraId = phase.getInfraDefinitionId();
      if (phase.checkInfraDefinitionTemplatized()) {
        expression = Preconditions.checkNotNull(phase.fetchInfraDefinitonTemplateExpression());
        if (expressionsHandled.contains(expression.getExpression())) {
          continue;
        }
        for (Map.Entry<String, String> entry : workflowVariables.entrySet()) {
          String varName = expression.getExpression().replace("${", "").replace("}", "");
          if (varName.equals(entry.getKey())) {
            infraId = entry.getValue();
            break;
          }
        }
        boolean isAdded = addResourceConstraint(appId, canaryOrchestrationWorkflow, phase, infraId);
        if (isAdded) {
          expressionsHandled.add(expression.getExpression());
          resourceConstraintAdded = true;
        }
      } else {
        boolean isAdded = addResourceConstraint(appId, canaryOrchestrationWorkflow, phase, infraId);
        if (isAdded) {
          resourceConstraintAdded = true;
        }
      }
    }
    return resourceConstraintAdded;
  }

  private boolean addResourceConstraint(String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow,
      WorkflowPhase workflowPhase, String infraId) {
    boolean resourceConstraintAdded = false;
    boolean isDynamicInfra = infrastructureDefinitionService.isDynamicInfrastructure(appId, infraId);
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    if (addAsFirstStepInPhaseStep(canaryOrchestrationWorkflow, isDynamicInfra, phaseSteps)) {
      PhaseStep firstStep = phaseSteps.get(0);
      firstStep.getSteps().add(
          0, getResourceConstraintStep(appId, canaryOrchestrationWorkflow.getConcurrencyStrategy()));
      resourceConstraintAdded = true;
    } else if (isDynamicInfra && isPresent(phaseSteps, getProvisionInfrastructurePhaseStepPredicate())) {
      PhaseStep provisionerPhaseStep =
          phaseSteps.get(fetchIndex(phaseSteps, getProvisionInfrastructurePhaseStepPredicate()));
      int provisionerStepIndex =
          fetchIndex(provisionerPhaseStep.getSteps(), step -> PROVISIONER_STEP_TYPES.contains(step.getType()));
      if (provisionerStepIndex > -1) {
        provisionerPhaseStep.getSteps().add(provisionerStepIndex + 1,
            getResourceConstraintStep(appId, canaryOrchestrationWorkflow.getConcurrencyStrategy()));
        resourceConstraintAdded = true;
      }
    } else {
      log.info("No Provisioner Step for Dynamic Infra. Skipping Adding Steps for Concurrency");
    }
    return resourceConstraintAdded;
  }

  private boolean addAsFirstStepInPhaseStep(
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow, boolean isDynamicInfra, List<PhaseStep> phaseSteps) {
    return (!isDynamicInfra && isNotEmpty(phaseSteps))
        || (canaryOrchestrationWorkflow.getPreDeploymentSteps() != null
            && isPresent(canaryOrchestrationWorkflow.getPreDeploymentSteps().getSteps(),
                step -> PROVISIONER_STEP_TYPES.contains(step.getType()))
            && isNotEmpty(phaseSteps));
  }

  @NotNull
  private Predicate<PhaseStep> getProvisionInfrastructurePhaseStepPredicate() {
    return phaseStep -> phaseStep.getPhaseStepType() == PhaseStepType.PROVISION_INFRASTRUCTURE;
  }

  public GraphNode getResourceConstraintStep(String appId, ConcurrencyStrategy concurrencyStrategy) {
    String accountId = appService.getAccountIdByAppId(appId);
    ResourceConstraint resourceConstraint =
        resourceConstraintService.ensureResourceConstraintForConcurrency(accountId, QUEUING_RC_NAME);
    return GraphNode.builder()
        .id(generateUuid())
        .type(RESOURCE_CONSTRAINT.name())
        .name(RC_INFRA_STEP_NAME)
        .properties(getResourceConstraintProperties(resourceConstraint, concurrencyStrategy))
        .build();
  }

  private ImmutableMap<String, Object> getResourceConstraintProperties(
      ResourceConstraint resourceConstraint, ConcurrencyStrategy concurrencyStrategy) {
    ImmutableMap.Builder<String, Object> mapBuilder =
        ImmutableMap.<String, Object>builder()
            .put(ResourceConstraintStateKeys.resourceConstraintId, resourceConstraint.getUuid())
            .put(ResourceConstraintStateKeys.permits, 1)
            .put(ResourceConstraintStateKeys.holdingScope, concurrencyStrategy.getHoldingScope().name())
            .put(ResourceConstraintStateKeys.resourceUnit, concurrencyStrategy.getResourceUnit())
            .put(ResourceConstraintStateKeys.acquireMode, AcquireMode.ENSURE)
            .put(ResourceConstraintStateKeys.harnessOwned, resourceConstraint.isHarnessOwned())
            .put(STATE_TIMEOUT_KEY_NAME, WEEK_TIMEOUT);
    if (isNotEmpty(concurrencyStrategy.getNotificationGroups())) {
      mapBuilder.put(ResourceConstraintStateKeys.notificationGroups, concurrencyStrategy.getNotificationGroups());
    }
    return mapBuilder.build();
  }

  private boolean concurrencyEnabled(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    ConcurrencyStrategy concurrencyStrategy = canaryOrchestrationWorkflow.getConcurrencyStrategy();
    return concurrencyStrategy != null && concurrencyStrategy.isEnabled();
  }
}
