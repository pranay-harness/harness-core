package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.BARRIERS_NOT_RUNNING_CONCURRENTLY;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.DEPLOY_CONTAINERS;
import static software.wings.sm.StateType.BARRIER;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.BarrierInstance;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BarrierService.OrchestrationWorkflowInfo;

import java.util.List;

public class BarrierServiceTest extends WingsBaseTest {
  @Inject private BarrierService barrierService;

  @Test
  public void shouldSave() {
    final BarrierInstance.Pipeline barrierPipeline =
        BarrierInstance.Pipeline.builder().executionId(generateUuid()).build();
    final BarrierInstance barrierInstance = BarrierInstance.builder().name("foo").pipeline(barrierPipeline).build();

    final String uuid = barrierService.save(barrierInstance);
    final BarrierInstance loadedBarrierInstance = barrierService.get(uuid);

    assertThat(loadedBarrierInstance.getName()).isEqualTo(barrierInstance.getName());
    assertThat(loadedBarrierInstance.getPipeline()).isEqualTo(barrierPipeline);
  }

  @Test
  public void testObtainInstancesWithNoBarriers() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
            .addWorkflowPhase(aWorkflowPhase().serviceId(SERVICE_ID).infraMappingId(INFRA_MAPPING_ID).build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo")
                   .pipelineStateId("bar")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid());

    assertThat(barrierInstances).isEmpty();
  }

  @Test
  public void testObtainInstancesWithSingleBarrier() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo")
                   .pipelineStateId("bar")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid());
    assertThat(barrierInstances).isEmpty();
  }

  @Test
  public void testObtainInstances() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow1 =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    final CanaryOrchestrationWorkflow orchestrationWorkflow2 =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    String pipelineExecution = generateUuid();
    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo1")
                   .pipelineStateId("bar1")
                   .orchestrationWorkflow(orchestrationWorkflow1)
                   .build(),
            OrchestrationWorkflowInfo.builder()
                .workflowId("foo2")
                .pipelineStateId("bar2")
                .orchestrationWorkflow(orchestrationWorkflow2)
                .build()),
        pipelineExecution);
    assertThat(barrierInstances.size()).isEqualTo(1);
    assertThat(barrierInstances.get(0).getName()).isEqualTo("deploy");
    assertThat(barrierInstances.get(0).getPipeline().getExecutionId()).isEqualTo(pipelineExecution);
  }

  private void assertThrowsNotRunningConcurrently(CanaryOrchestrationWorkflow orchestrationWorkflow) {
    assertThatThrownBy(()
                           -> barrierService.obtainInstances(APP_ID,
                               asList(OrchestrationWorkflowInfo.builder()
                                          .workflowId("foo1")
                                          .pipelineStateId("bar1")
                                          .orchestrationWorkflow(orchestrationWorkflow)
                                          .build()),
                               generateUuid()))
        .isInstanceOf(WingsException.class)
        .matches(ex -> ((WingsException) ex).getCode() == BARRIERS_NOT_RUNNING_CONCURRENTLY);
  }

  @Test
  public void testObtainInstancesWithTwoBarriersInTheSamePhaseStep() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    assertThrowsNotRunningConcurrently(orchestrationWorkflow);
  }

  @Test
  public void testObtainInstancesWithTwoBarriersInTheSameWorkflowPhase() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    assertThrowsNotRunningConcurrently(orchestrationWorkflow);
  }

  @Test
  public void testObtainInstancesWithDifferentBarriers() {
    final CanaryOrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
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
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
            .build();

    final List<BarrierInstance> barrierInstances = barrierService.obtainInstances(APP_ID,
        asList(OrchestrationWorkflowInfo.builder()
                   .workflowId("foo1")
                   .pipelineStateId("bar1")
                   .orchestrationWorkflow(orchestrationWorkflow)
                   .build()),
        generateUuid());
    assertThat(barrierInstances).isEmpty();
  }
}
