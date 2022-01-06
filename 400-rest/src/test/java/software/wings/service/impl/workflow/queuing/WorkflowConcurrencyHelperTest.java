/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.queuing;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static software.wings.common.InfrastructureConstants.RC_INFRA_STEP_NAME;
import static software.wings.infra.InfraDefinitionTestConstants.RESOURCE_CONSTRAINT_NAME;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructCanaryWorkflowWithConcurrencyStrategy;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructCanaryWorkflowWithPhase;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.ResourceConstraint;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.infrastructuredefinition.InfrastructureDefinitionServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceTestHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ResourceConstraintService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class WorkflowConcurrencyHelperTest extends WingsBaseTest {
  @Mock private ResourceConstraintService resourceConstraintService;
  @Mock private AppService appService;
  @Mock private InfrastructureDefinitionServiceImpl infrastructureDefinitionService;

  @InjectMocks @Inject private WorkflowConcurrencyHelper workflowConcurrencyHelper;

  @Before
  public void setUp() throws Exception {
    ResourceConstraint resourceConstraint = ResourceConstraint.builder()
                                                .uuid(generateUuid())
                                                .name(RESOURCE_CONSTRAINT_NAME)
                                                .accountId(ACCOUNT_ID)
                                                .capacity(1)
                                                .strategy(Strategy.FIFO)
                                                .build();

    when(resourceConstraintService.ensureResourceConstraintForConcurrency(ACCOUNT_ID, RESOURCE_CONSTRAINT_NAME))
        .thenReturn(resourceConstraint);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void noConcurrencyIfStrategyNotPresent() {
    Workflow workflow = constructCanaryWorkflowWithPhase();
    when(appService.getAccountIdByAppId(workflow.getAppId())).thenReturn(workflow.getAccountId());
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid");

    workflowConcurrencyHelper.enhanceWithConcurrencySteps(workflow, new HashMap<>());

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();

    WorkflowPhase workflowPhase = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(0);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void ensureConcurrencyForAlreadyProvisioned() {
    when(infrastructureDefinitionService.isDynamicInfrastructure(APP_ID, INFRA_DEFINITION_ID)).thenReturn(false);
    Workflow workflow = constructCanaryWorkflowWithConcurrencyStrategy();
    when(appService.getAccountIdByAppId(workflow.getAppId())).thenReturn(workflow.getAccountId());
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid");
    List<WorkflowPhase> workflowPhasesBeforeQueuing =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhaseBeforeQueuing = workflowPhasesBeforeQueuing.get(workflowPhasesBeforeQueuing.size() - 1);
    assertThat(workflowPhaseBeforeQueuing.getPhaseSteps()).isNotNull().hasSize(6);
    assertThat(workflowPhaseBeforeQueuing.getPhaseSteps().get(0).getSteps()).isNotNull().hasSize(1);

    workflowConcurrencyHelper.enhanceWithConcurrencySteps(workflow, new HashMap<>());

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();

    WorkflowPhase workflowPhase = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(6);
    assertThat(workflowPhaseBeforeQueuing.getPhaseSteps().get(0).getSteps()).isNotNull().hasSize(2);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps().get(0))
        .hasFieldOrPropertyWithValue("name", RC_INFRA_STEP_NAME);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void ensureConcurrencyForDynamicallyProvisionedWithProvisionStep() {
    when(infrastructureDefinitionService.isDynamicInfrastructure(APP_ID, INFRA_DEFINITION_ID)).thenReturn(true);
    Workflow workflow = WorkflowServiceTestHelper.constructBasicWorkflowWithThrottling(true);
    when(appService.getAccountIdByAppId(workflow.getAppId())).thenReturn(workflow.getAccountId());
    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid");
    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(5);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps()).isNotNull().hasSize(1);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps().get(0))
        .hasFieldOrPropertyWithValue("name", "CloudFormation Create Stack");

    workflowConcurrencyHelper.enhanceWithConcurrencySteps(workflow, new HashMap<>());

    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(5);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps()).isNotNull().hasSize(2);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps().get(1))
        .hasFieldOrPropertyWithValue("name", RC_INFRA_STEP_NAME);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void ensureConcurrencyForDynamicallyProvisionedWithNoProvisionStep() {
    when(infrastructureDefinitionService.isDynamicInfrastructure(APP_ID, INFRA_DEFINITION_ID)).thenReturn(true);
    Workflow workflow = WorkflowServiceTestHelper.constructCanaryWorkflowWithConcurrencyStrategy();
    when(appService.getAccountIdByAppId(workflow.getAppId())).thenReturn(workflow.getAccountId());
    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid");
    assertThat(workflowPhases).isNotNull().hasSize(1);
    assertThat(workflow.getOrchestrationWorkflow()).isNotNull();

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    assertThat(canaryOrchestrationWorkflow.getPreDeploymentSteps()).isNotNull();
    assertThat(canaryOrchestrationWorkflow.getPreDeploymentSteps().getSteps()).isNotNull().hasSize(1);
    assertThat(canaryOrchestrationWorkflow.getPreDeploymentSteps().getSteps().get(0))
        .hasFieldOrPropertyWithValue("name", "Terraform Provision");

    WorkflowPhase workflowPhase = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(6);

    workflowConcurrencyHelper.enhanceWithConcurrencySteps(workflow, new HashMap<>());

    assertThat(workflowPhase.getPhaseSteps()).isNotNull().hasSize(6);
    assertThat(workflowPhase.getPhaseSteps().get(0).getSteps().get(0))
        .hasFieldOrPropertyWithValue("name", RC_INFRA_STEP_NAME);
  }
}
