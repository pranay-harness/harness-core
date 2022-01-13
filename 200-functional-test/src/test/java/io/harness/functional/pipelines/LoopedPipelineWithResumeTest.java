/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.pipelines;

import static io.harness.functional.WorkflowUtils.getTemplateExpressionsForInfraDefinition;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.POOJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.PipelineStageGroupedInfo;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LoopedPipelineWithResumeTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private Application application;
  private Service service;
  private Environment environment;
  private Pipeline savedPipeline;
  private InfrastructureDefinition infrastructureDefinition1;
  private InfrastructureDefinition infrastructureDefinition2;

  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void createPipeline() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition1 =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureType.PDC, bearerToken);
    infrastructureDefinition2 = infrastructureDefinitionGenerator.ensurePDC(seed, owners, "PDC2");

    resetCache(application.getAccountId());
    Workflow templatizedWorkflow1 = getWorkflow("exit 0", "LoopTest1 ");
    Workflow templatizedWorkflow2 = getWorkflow("exit 1", "LoopTest2 ");
    Workflow templatizedWorkflow3 = getWorkflow("exit 0", "LoopTest3 ");

    String pipelineName = "Looped-Pipeline-" + System.currentTimeMillis();

    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    Pipeline createdPipeline =
        PipelineRestUtils.createPipeline(application.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(createdPipeline).isNotNull();

    ImmutableMap<String, String> workflowVariables =
        ImmutableMap.<String, String>builder().put("InfraDefinition_KUBERNETES", "${infra}").build();

    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage executionStage =
        PipelineUtils.prepareExecutionStage(environment.getUuid(), templatizedWorkflow1.getUuid(), workflowVariables);

    ImmutableMap<String, String> workflowVariables2 =
        ImmutableMap.<String, String>builder().put("InfraDefinition_KUBERNETES", "${infra2}").build();
    PipelineStage executionStage2 =
        PipelineUtils.prepareExecutionStage(environment.getUuid(), templatizedWorkflow3.getUuid(), workflowVariables2);
    executionStage2.setParallel(true);
    PipelineStage executionStage3 =
        PipelineUtils.prepareExecutionStage(environment.getUuid(), templatizedWorkflow2.getUuid(), workflowVariables2);
    pipelineStages.add(executionStage);
    pipelineStages.add(executionStage2);
    pipelineStages.add(executionStage3);
    createdPipeline.setPipelineStages(pipelineStages);

    savedPipeline = PipelineRestUtils.updatePipeline(application.getAppId(), createdPipeline, bearerToken);
  }

  @NotNull
  private Workflow getWorkflow(String script, String name) {
    Workflow workflow1 =
        workflowUtils.getRollingK8sWorkflow(name + System.currentTimeMillis(), service, infrastructureDefinition1);
    Workflow savedWorkflow1 =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow1);

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow1.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhases().get(0);
    PhaseStep deploy = workflowPhase.getPhaseSteps().get(2);
    Map<String, Object> properties = new HashMap<>();

    properties.put("scriptString", script);
    properties.put("scriptType", "BASH");
    properties.put("timeoutMillis", 60000);
    properties.put("publishAsVar", false);
    properties.put("executeOnDelegate", true);
    GraphNode shellScriptNode =
        GraphNode.builder().properties(properties).type("SHELL_SCRIPT").name("Shell script").build();
    deploy.setSteps(Collections.singletonList(shellScriptNode));
    WorkflowPhase updatedPhase = WorkflowRestUtils.updateWorkflowPhase(bearerToken, application.getAccountId(),
        application.getUuid(), savedWorkflow1.getUuid(), workflowPhase.getUuid(), workflowPhase);
    savedWorkflow1 = WorkflowRestUtils.getWorkflow(application.getUuid(), savedWorkflow1.getUuid(), bearerToken);
    savedWorkflow1.setTemplateExpressions(
        Collections.singletonList(getTemplateExpressionsForInfraDefinition("${InfraDefinition_KUBERNETES}")));
    Workflow templatizedWorkflow = WorkflowRestUtils.updateWorkflow(
        bearerToken, application.getAccountId(), application.getUuid(), savedWorkflow1);
    assertThat(templatizedWorkflow.isTemplatized()).isTrue();
    return templatizedWorkflow;
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("Failing to deserialize context element. Will Check the issue")
  public void triggerPipelineFailureWithResumeAPI() {
    ImmutableMap<String, String> pipelineVariables =
        ImmutableMap.<String, String>builder()
            .put("infra", String.join(",", infrastructureDefinition1.getUuid(), infrastructureDefinition2.getUuid()))
            .put("infra2", String.join(",", infrastructureDefinition1.getUuid()))
            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setPipelineId(savedPipeline.getUuid());
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    executionArgs.setWorkflowVariables(pipelineVariables);

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    List<PipelineStageGroupedInfo> resumeStages = PipelineRestUtils.getResumeStages(
        application.getUuid(), application.getAccountId(), workflowExecution.getUuid(), bearerToken);

    assertThat(resumeStages).isNotNull();
    assertThat(resumeStages.size()).isEqualTo(2);
    assertThat(resumeStages.get(0).getName()).isEqualTo(savedPipeline.getPipelineStages().get(0).getName());
    assertThat(resumeStages.get(1).getName()).isEqualTo(savedPipeline.getPipelineStages().get(2).getName());
    assertThat(resumeStages.get(0).getPipelineStageElementNames().get(0))
        .isEqualTo(savedPipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getName());
    assertThat(resumeStages.get(0).getPipelineStageElementNames().get(1))
        .isEqualTo(savedPipeline.getPipelineStages().get(1).getPipelineStageElements().get(0).getName());
    assertThat(resumeStages.get(1).getPipelineStageElementNames().get(0))
        .isEqualTo(savedPipeline.getPipelineStages().get(2).getPipelineStageElements().get(0).getName());

    WorkflowExecution resumedExecution = PipelineRestUtils.resumePipeline(
        bearerToken, application.getUuid(), application.getAccountId(), workflowExecution.getUuid(), 2);
    waitForWorkflowExecutionCompletion(resumedExecution);
    resumedExecution = workflowExecutionService.getWorkflowExecution(application.getUuid(), resumedExecution.getUuid());
    assertThat(resumedExecution.getPipelineExecution()).isNotNull();
    assertThat(resumedExecution.getPipelineExecution().getPipelineStageExecutions().size()).isEqualTo(4);
    List<PipelineStageExecution> pipelineStageExecutions =
        resumedExecution.getPipelineExecution().getPipelineStageExecutions();
    PipelineStageExecution p1 = pipelineStageExecutions.get(0);
    PipelineStageExecution p2 = pipelineStageExecutions.get(1);
    assertThat(p1.getPipelineStageElementId()).isNotEmpty();
    assertThat(p1.getPipelineStageElementId()).isEqualTo(p2.getPipelineStageElementId());
    assertThat(p1.getParallelInfo()).isNotNull();
    assertThat(p1.getParallelInfo().getGroupIndex()).isEqualTo(1);
    assertThat(p2.getParallelInfo()).isNotNull();
    assertThat(p2.getParallelInfo().getGroupIndex()).isEqualTo(1);
  }

  private void waitForWorkflowExecutionCompletion(WorkflowExecution workflowExecution) {
    Awaitility.await()
        .atMost(5, TimeUnit.MINUTES)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> ExecutionStatus.isFinalStatus(
                       workflowExecutionService.getWorkflowExecution(application.getUuid(), workflowExecution.getUuid())
                           .getStatus()));
  }
}
