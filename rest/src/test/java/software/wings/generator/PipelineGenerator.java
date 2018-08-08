package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Pipeline.PipelineBuilder;
import static software.wings.beans.Pipeline.builder;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.WorkflowGenerator.PostProcessInfo;
import software.wings.service.intfc.PipelineService;

@Singleton
public class PipelineGenerator {
  @Inject PipelineService pipelineService;

  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;

  public enum Pipelines { BARRIER }

  public Pipeline ensurePredefined(Randomizer.Seed seed, Owners owners, Pipelines predefined) {
    switch (predefined) {
      case BARRIER:
        return ensureBarrier(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Pipeline ensureBarrier(Randomizer.Seed seed, Owners owners) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    Workflow[][] workflows = new Workflow[2][2];

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        workflows[i][j] = workflowGenerator.ensureWorkflow(seed, owners,
            aWorkflow()
                .withName(format("Barrier Parallel Section %d-%d", i + 1, j + 1))
                .withWorkflowType(WorkflowType.ORCHESTRATION)
                .withInfraMappingId(infrastructureMapping.getUuid())
                .withOrchestrationWorkflow(
                    aBasicOrchestrationWorkflow()
                        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                        .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                        .build())
                .build());

        workflows[i][j] =
            workflowGenerator.postProcess(workflows[i][j], PostProcessInfo.builder().selectNodeCount(2).build());
      }
    }

    return ensurePipeline(seed, owners,
        Pipeline.builder()
            .name("Barrier Pipeline")
            .pipelineStages(asList(
                PipelineStage.builder()
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Parallel section 1-1")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", workflows[0][0].getEnvId(),
                                                          "workflowId", workflows[0][0].getUuid()))
                                                      .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Parallel section 1-2")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", workflows[0][1].getEnvId(),
                                                          "workflowId", workflows[0][1].getUuid()))
                                                      .build()))
                    .build(),
                PipelineStage.builder()
                    .pipelineStageElements(
                        asList(PipelineStageElement.builder()
                                   .name("Parallel section 2-1")
                                   .type(ENV_STATE.name())
                                   .properties(ImmutableMap.of(
                                       "envId", workflows[1][0].getEnvId(), "workflowId", workflows[1][0].getUuid()))
                                   .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(
                        asList(PipelineStageElement.builder()
                                   .name("Parallel section 2-2")
                                   .type(ENV_STATE.name())
                                   .properties(ImmutableMap.of(
                                       "envId", workflows[1][1].getEnvId(), "workflowId", workflows[1][1].getUuid()))
                                   .build()))
                    .build()))
            .build());
  }

  public Pipeline ensurePipeline(Randomizer.Seed seed, Owners owners, Pipeline pipeline) {
    EnhancedRandom random = Randomizer.instance(seed);

    final PipelineBuilder builder = builder();

    if (pipeline != null && pipeline.getAppId() != null) {
      builder.appId(pipeline.getAppId());
    } else {
      final Application application = owners.obtainApplication();
      builder.appId(application.getUuid());
    }

    if (pipeline != null && pipeline.getName() != null) {
      builder.name(pipeline.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (pipeline != null && pipeline.getPipelineStages() != null) {
      builder.pipelineStages(pipeline.getPipelineStages());
    } else {
      throw new UnsupportedOperationException();
    }

    return pipelineService.save(builder.build());
  }
}
