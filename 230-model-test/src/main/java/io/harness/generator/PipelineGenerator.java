/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static io.harness.generator.PipelineGenerator.Pipelines.BASIC;
import static io.harness.generator.PipelineGenerator.Pipelines.BUILD;
import static io.harness.generator.PipelineGenerator.Pipelines.RESOURCE_CONSTRAINT_WORKFLOW;
import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Pipeline.PipelineBuilder;
import static software.wings.beans.Pipeline.builder;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.RESOURCE_CONSTRAINT;
import static software.wings.sm.states.ResourceConstraintState.NotificationEvent.BLOCKED;
import static software.wings.sm.states.ResourceConstraintState.NotificationEvent.UNBLOCKED;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import io.harness.beans.ResourceConstraint;
import io.harness.beans.WorkflowType;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator.PostProcessInfo;
import io.harness.generator.WorkflowGenerator.Workflows;

import software.wings.beans.Application;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.states.HoldingScope;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PipelineGenerator {
  @Inject private PipelineService pipelineService;

  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private ServiceGenerator serviceGenerator;

  public Pipeline ensurePredefined(Randomizer.Seed seed, Owners owners, Pipelines predefined) {
    switch (predefined) {
      case BARRIER:
        return ensureBarrier(seed, owners);
      case RESOURCE_CONSTRAINT_WORKFLOW:
        return ensureResourceConstraintWorkflow(seed, owners);
      case BUILD:
        return ensureBuildPipeline(seed, owners);
      case BASIC:
        return ensureBasicPipeline(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Pipeline ensureBasicPipeline(Randomizer.Seed seed, Owners owners) {
    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AWS_SSH_TEST);
    Workflow workflow = aWorkflow()
                            .name("Test workflow")
                            .infraDefinitionId(infrastructureDefinition.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                       .build())
                            .build();
    Workflow buildWorkflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);

    return ensurePipeline(seed, owners,
        Pipeline.builder()
            .name(BASIC.name())
            .pipelineStages(asList(
                PipelineStage.builder()
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Build")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("workflowId", buildWorkflow.getUuid(),
                                                          "envId", buildWorkflow.getEnvId()))
                                                      .build()))
                    .build()))
            .build());
  }

  public Pipeline ensureBarrier(Randomizer.Seed seed, Owners owners) {
    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AWS_SSH_TEST);

    Workflow[][] workflows = new Workflow[2][2];

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        workflows[i][j] = workflowGenerator.ensureWorkflow(seed, owners,
            aWorkflow()
                .name(format("Barrier Parallel Section %d-%d", i + 1, j + 1))
                .workflowType(WorkflowType.ORCHESTRATION)
                .infraDefinitionId(infrastructureDefinition.getUuid())
                .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                           .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                           .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                           .build())
                .build());

        workflows[i][j] =
            workflowGenerator.postProcess(workflows[i][j], PostProcessInfo.builder().selectNodeCount(2).build());
      }
    }

    return ensurePipeline(seed, owners,
        Pipeline.builder()
            .name(BARRIER.name())
            .description("This is pipeline to test barriers")
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

  public Pipeline ensureResourceConstraintWorkflow(Randomizer.Seed seed, Owners owners) {
    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AWS_SSH_TEST);

    final ResourceConstraint asapResourceConstraint = resourceConstraintGenerator.ensureResourceConstraint(seed, owners,
        ResourceConstraint.builder()
            .name(RESOURCE_CONSTRAINT_WORKFLOW.name())
            .capacity(10)
            .strategy(Strategy.ASAP)
            .build());

    serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);

    Workflow[] workflows = new Workflow[3];

    for (int i = 0; i < 3; i++) {
      workflows[i] = workflowGenerator.ensureWorkflow(seed, owners,
          aWorkflow()
              .name(format("Resource Constraint %d", i + 1))
              .workflowType(WorkflowType.ORCHESTRATION)
              .infraDefinitionId(infrastructureDefinition.getUuid())
              .orchestrationWorkflow(
                  aBasicOrchestrationWorkflow()
                      .withPreDeploymentSteps(
                          aPhaseStep(PRE_DEPLOYMENT)
                              .addStep(
                                  GraphNode.builder()
                                      .type(RESOURCE_CONSTRAINT.name())
                                      .name(asapResourceConstraint.getName())
                                      .properties(ImmutableMap.<String, Object>builder()
                                                      .put("resourceConstraintId", asapResourceConstraint.getUuid())
                                                      .put("resourceUnit", "unit" + i / 2)
                                                      .put("permits", 6)
                                                      .put("holdingScope", HoldingScope.WORKFLOW.name())
                                                      .put("notificationEvents", asList(BLOCKED, UNBLOCKED))
                                                      .build())
                                      .build())
                              .build())
                      .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                      .build())
              .build());

      workflows[i] = workflowGenerator.postProcess(workflows[i], PostProcessInfo.builder().selectNodeCount(2).build());
    }

    return ensurePipeline(seed, owners,
        Pipeline.builder()
            .name(RESOURCE_CONSTRAINT_WORKFLOW.name())
            .pipelineStages(asList(
                PipelineStage.builder()
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Resource Constraint 1")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", workflows[0].getEnvId(),
                                                          "workflowId", workflows[0].getUuid()))
                                                      .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Resource Constraint 2")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", workflows[1].getEnvId(),
                                                          "workflowId", workflows[1].getUuid()))
                                                      .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("Resource Constraint 3")
                                                      .type(ENV_STATE.name())
                                                      .properties(ImmutableMap.of("envId", workflows[2].getEnvId(),
                                                          "workflowId", workflows[2].getUuid()))
                                                      .build()))
                    .build()))
            .build());
  }

  public Pipeline ensureBuildPipeline(Randomizer.Seed seed, Owners owners) {
    Workflow buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);

    return ensurePipeline(seed, owners,
        Pipeline.builder()
            .name(BUILD.name())
            .pipelineStages(asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("Build")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("workflowId", buildWorkflow.getUuid()))
                                               .build()))
                                       .build()))
            .build());
  }

  public enum Pipelines { BARRIER, RESOURCE_CONSTRAINT_WORKFLOW, BUILD, BASIC }

  public Pipeline ensurePipeline(Randomizer.Seed seed, Owners owners, Pipeline pipeline) {
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

    if (pipeline.getDescription() != null) {
      builder.description(pipeline.getDescription());
    }

    builder.pipelineStages(pipeline.getPipelineStages());

    return GeneratorUtils.suppressDuplicateException(()
                                                         -> pipelineService.save(builder.build()),
        () -> pipelineService.getPipelineByName(builder.build().getAppId(), pipeline.getName()));
  }
}
