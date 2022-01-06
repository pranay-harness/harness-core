/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.BARRIERS_NOT_RUNNING_CONCURRENTLY;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.DEPLOY_CONTAINERS;
import static software.wings.sm.StateType.BARRIER;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.BarrierInstance;
import software.wings.beans.BarrierInstancePipeline;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStepType;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BarrierService.OrchestrationWorkflowInfo;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BarrierServiceTest extends WingsBaseTest {
  @Inject private BarrierService barrierService;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSave() {
    final BarrierInstancePipeline barrierPipeline =
        BarrierInstancePipeline.builder().executionId(generateUuid()).build();
    final BarrierInstance barrierInstance = BarrierInstance.builder().name("foo").pipeline(barrierPipeline).build();

    final String uuid = barrierService.save(barrierInstance);
    final BarrierInstance loadedBarrierInstance = barrierService.get(uuid);

    assertThat(loadedBarrierInstance.getName()).isEqualTo(barrierInstance.getName());
    assertThat(loadedBarrierInstance.getPipeline()).isEqualTo(barrierPipeline);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstancesWithNoBarriers() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(aWorkflowPhase().serviceId(SERVICE_ID).infraMappingId(INFRA_MAPPING_ID).build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo")
                   .pipelineStageId("bar")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid(), 0);

    assertThat(barrierInstances).isEmpty();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstancesWithSingleBarrier() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo")
                   .pipelineStageId("bar")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid(), 0);
    assertThat(barrierInstances).isEmpty();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstances() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow1 =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    final CanaryOrchestrationWorkflow orchestrationWorkflow2 =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    String pipelineExecution = generateUuid();
    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo1")
                   .pipelineStageId("bar1")
                   .orchestrationWorkflow(orchestrationWorkflow1)
                   .build(),
            OrchestrationWorkflowInfo.builder()
                .workflowId("foo2")
                .pipelineStageId("bar2")
                .orchestrationWorkflow(orchestrationWorkflow2)
                .build()),
        pipelineExecution, 0);
    assertThat(barrierInstances.size()).isEqualTo(1);
    assertThat(barrierInstances.get(0).getName()).isEqualTo("deploy");
    assertThat(barrierInstances.get(0).getPipeline().getExecutionId()).isEqualTo(pipelineExecution);
  }

  private void assertThrowsNotRunningConcurrently(CanaryOrchestrationWorkflow orchestrationWorkflow) {
    assertThatThrownBy(()
                           -> barrierService.obtainInstances(APP_ID,
                               asList(OrchestrationWorkflowInfo.builder()
                                          .workflowId("foo1")
                                          .pipelineStageId("bar1")
                                          .orchestrationWorkflow(orchestrationWorkflow)
                                          .build()),
                               generateUuid(), 0))
        .isInstanceOf(WingsException.class)
        .matches(ex -> ((WingsException) ex).getCode() == BARRIERS_NOT_RUNNING_CONCURRENTLY);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstancesWithTwoBarriersInTheSamePhaseStep() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThrowsNotRunningConcurrently(orchestrationWorkflow);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstancesWithTwoBarriersInTheSameWorkflowPhase() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build(),
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(GraphNode.builder()
                                         .id(generateUuid())
                                         .type(BARRIER.name())
                                         .name("Barrier")
                                         .properties(
                                             ImmutableMap.<String, Object>builder().put("identifier", "deploy").build())
                                         .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThrowsNotRunningConcurrently(orchestrationWorkflow);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObtainInstancesWithDifferentBarriers() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(
                aWorkflowPhase()
                    .serviceId(SERVICE_ID)
                    .infraMappingId(INFRA_MAPPING_ID)
                    .phaseSteps(asList(
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(
                                GraphNode.builder()
                                    .id(generateUuid())
                                    .type(BARRIER.name())
                                    .name("Barrier")
                                    .properties(
                                        ImmutableMap.<String, Object>builder().put("identifier", "deploy1").build())
                                    .build())
                            .build(),
                        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                            .addStep(
                                GraphNode.builder()
                                    .id(generateUuid())
                                    .type(BARRIER.name())
                                    .name("Barrier")
                                    .properties(
                                        ImmutableMap.<String, Object>builder().put("identifier", "deploy2").build())
                                    .build())
                            .build()))
                    .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo1")
                   .pipelineStageId("bar1")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid(), 0);
    assertThat(barrierInstances).isEmpty();
  }
}
