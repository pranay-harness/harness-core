/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.ExecutionInterruptType.ABORT;
import static io.harness.beans.ExecutionInterruptType.END_EXECUTION;
import static io.harness.beans.RepairActionCode.IGNORE;
import static io.harness.beans.RepairActionCode.MANUAL_INTERVENTION;
import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStepType;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class SetDefaultTimeOutAndActionForManualInterventionFailureStrategyTest extends CategoryTest {
  private static final long DEFAULT_TIMEOUT = 1209600000L;
  private final io.harness.migrations.all.SetDefaultTimeOutAndActionForManualInterventionFailureStrategy migration =
      new SetDefaultTimeOutAndActionForManualInterventionFailureStrategy();
  private final FailureStrategy migrated = FailureStrategy.builder()
                                               .repairActionCode(MANUAL_INTERVENTION)
                                               .manualInterventionTimeout(DEFAULT_TIMEOUT)
                                               .actionAfterTimeout(END_EXECUTION)
                                               .build();
  private final FailureStrategy notMigrated = FailureStrategy.builder().repairActionCode(MANUAL_INTERVENTION).build();
  private final List<FailureStrategy> singleFailureStrategy = Collections.singletonList(notMigrated);
  private final List<FailureStrategy> multipleFailureStrategies = Arrays.asList(
      notMigrated, FailureStrategy.builder().repairActionCode(IGNORE).build(), notMigrated.toBuilder().build());
  CanaryOrchestrationWorkflowBuilder workflowBuilder =
      CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow();
  private CanaryOrchestrationWorkflow workflow;

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckIfFailureStrategyMigrationRequiredAndReturnFalse() {
    boolean upgradeRequiredForNull = migration.migrationRequired(null);
    assertThat(upgradeRequiredForNull).isFalse();
    boolean upgradeRequiredForEmpty = migration.migrationRequired(Collections.emptyList());
    assertThat(upgradeRequiredForEmpty).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifySingleOrchestrationLevelFailureStrategy() {
    workflow = workflowBuilder.withFailureStrategies(singleFailureStrategy).build();
    boolean workflowUpdated = migration.checkAndModifyOrchestrationLevelFailureStrategies(workflow);
    assertSingleUpdated(workflowUpdated, workflow.getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifyMultipleOrchestrationLevelFailureStrategies() {
    workflow = workflowBuilder.withFailureStrategies(multipleFailureStrategies).build();
    boolean workflowUpdated = migration.checkAndModifyOrchestrationLevelFailureStrategies(workflow);
    assertMultipleUpdated(workflowUpdated, workflow.getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldNotModifyValidManualIntervention() {
    List<FailureStrategy> failureStrategies = Collections.singletonList(FailureStrategy.builder()
                                                                            .repairActionCode(MANUAL_INTERVENTION)
                                                                            .actionAfterTimeout(ABORT)
                                                                            .manualInterventionTimeout(86400001L)
                                                                            .build());
    workflow = workflowBuilder.withFailureStrategies(failureStrategies).build();
    boolean workflowUpdated = migration.checkAndModifyOrchestrationLevelFailureStrategies(workflow);
    assertThat(workflowUpdated).isTrue();
    assertThat(workflow.getFailureStrategies().size()).isEqualTo(1);
    assertThat(workflow.getFailureStrategies().get(0).getActionAfterTimeout()).isEqualTo(ABORT);
    assertThat(workflow.getFailureStrategies().get(0).getManualInterventionTimeout()).isEqualTo(86400001L);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifySinglePhaseLevelFailureStrategy() {
    List<WorkflowPhase> workflowPhases = Collections.singletonList(
        WorkflowPhaseBuilder.aWorkflowPhase()
            .phaseSteps(Collections.singletonList(PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS)
                                                      .withFailureStrategies(singleFailureStrategy)
                                                      .build()))
            .build());
    workflow = workflowBuilder.withWorkflowPhases(workflowPhases).build();
    boolean workflowUpdated = migration.checkAndModifyPhaseLevelFailureStrategies(workflow);
    assertSingleUpdated(
        workflowUpdated, workflow.getWorkflowPhases().get(0).getPhaseSteps().get(0).getFailureStrategies());
  }
  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifyMultiplePhaseLevelFailureStrategies() {
    List<WorkflowPhase> workflowPhases = Collections.singletonList(
        WorkflowPhaseBuilder.aWorkflowPhase()
            .phaseSteps(Collections.singletonList(PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS)
                                                      .withFailureStrategies(multipleFailureStrategies)
                                                      .build()))
            .build());
    workflow = workflowBuilder.withWorkflowPhases(workflowPhases).build();
    boolean workflowUpdated = migration.checkAndModifyPhaseLevelFailureStrategies(workflow);
    assertMultipleUpdated(
        workflowUpdated, workflow.getWorkflowPhases().get(0).getPhaseSteps().get(0).getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifySinglePreDeploymentFailureStrategy() {
    PhaseStep phaseStep =
        PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS).withFailureStrategies(singleFailureStrategy).build();
    workflow = workflowBuilder.withPreDeploymentSteps(phaseStep).build();
    boolean workflowUpdated = migration.checkAndModifyPreDeploymentStepsLevelFailureStrategies(workflow);
    assertSingleUpdated(workflowUpdated, workflow.getPreDeploymentSteps().getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifyMultiplePreDeploymentFailureStrategy() {
    PhaseStep phaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS)
                              .withFailureStrategies(multipleFailureStrategies)
                              .build();
    workflow = workflowBuilder.withPreDeploymentSteps(phaseStep).build();
    boolean workflowUpdated = migration.checkAndModifyPreDeploymentStepsLevelFailureStrategies(workflow);
    assertMultipleUpdated(workflowUpdated, workflow.getPreDeploymentSteps().getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifySinglePostDeploymentFailureStrategy() {
    PhaseStep phaseStep =
        PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS).withFailureStrategies(singleFailureStrategy).build();
    workflow = workflowBuilder.withPostDeploymentSteps(phaseStep).build();
    boolean workflowUpdated = migration.checkAndModifyPostDeploymentStepsLevelFailureStrategies(workflow);
    assertSingleUpdated(workflowUpdated, workflow.getPostDeploymentSteps().getFailureStrategies());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldCheckAndModifyMultiplePostDeploymentFailureStrategy() {
    PhaseStep phaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.PREPARE_STEPS)
                              .withFailureStrategies(multipleFailureStrategies)
                              .build();
    workflow = workflowBuilder.withPostDeploymentSteps(phaseStep).build();
    boolean workflowUpdated = migration.checkAndModifyPostDeploymentStepsLevelFailureStrategies(workflow);
    assertMultipleUpdated(workflowUpdated, workflow.getPostDeploymentSteps().getFailureStrategies());
  }

  private void assertSingleUpdated(boolean workflowUpdated, List<FailureStrategy> failureStrategies) {
    assertThat(workflowUpdated).isTrue();
    assertThat(failureStrategies).containsOnly(migrated);
  }

  private void assertMultipleUpdated(boolean workflowUpdated, List<FailureStrategy> failureStrategies) {
    assertThat(workflowUpdated).isTrue();
    assertThat(failureStrategies.size()).isEqualTo(3);
    List<FailureStrategy> manualInterventions =
        failureStrategies.stream()
            .filter(failureStrategy -> failureStrategy.getRepairActionCode().equals(MANUAL_INTERVENTION))
            .collect(Collectors.toList());
    assertThat(manualInterventions.size()).isEqualTo(2);
    manualInterventions.forEach(manualIntervention -> assertThat(manualIntervention).isEqualTo(migrated));
  }
}
