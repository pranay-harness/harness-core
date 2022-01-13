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
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.infra.InfrastructureDefinition;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LoopedPipelineTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private FeatureFlagService featureFlagService;

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
    Workflow workflow = workflowUtils.getRollingK8sWorkflow(
        "Looped-Pipeline" + System.currentTimeMillis(), service, infrastructureDefinition1);
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhases().get(0);
    PhaseStep deploy = workflowPhase.getPhaseSteps().get(2);
    Map<String, Object> properties = new HashMap<>();

    properties.put("scriptString", "exit 0");
    properties.put("scriptType", "BASH");
    properties.put("timeoutMillis", 60000);
    properties.put("publishAsVar", false);
    properties.put("executeOnDelegate", true);
    GraphNode shellScriptNode =
        GraphNode.builder().properties(properties).type("SHELL_SCRIPT").name("Shell script").build();
    deploy.setSteps(Collections.singletonList(shellScriptNode));
    WorkflowPhase updatedPhase = WorkflowRestUtils.updateWorkflowPhase(bearerToken, application.getAccountId(),
        application.getUuid(), savedWorkflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
    savedWorkflow = WorkflowRestUtils.getWorkflow(application.getUuid(), savedWorkflow.getUuid(), bearerToken);
    savedWorkflow.setTemplateExpressions(
        Collections.singletonList(getTemplateExpressionsForInfraDefinition("${InfraDefinition_KUBERNETES}")));
    Workflow templatizedWorkflow =
        WorkflowRestUtils.updateWorkflow(bearerToken, application.getAccountId(), application.getUuid(), savedWorkflow);
    assertThat(templatizedWorkflow.isTemplatized()).isTrue();

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
        PipelineUtils.prepareExecutionStage(environment.getUuid(), templatizedWorkflow.getUuid(), workflowVariables);
    pipelineStages.add(executionStage);
    createdPipeline.setPipelineStages(pipelineStages);

    savedPipeline = PipelineRestUtils.updatePipeline(application.getAppId(), createdPipeline, bearerToken);
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("Failing to deserialize context element. Will Check the issue")
  public void triggerPipelineWithLoopRestAPI() {
    ImmutableMap<String, String> pipelineVariables =
        ImmutableMap.<String, String>builder()
            .put("infra", String.join(",", infrastructureDefinition1.getUuid(), infrastructureDefinition2.getUuid()))
            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setPipelineId(savedPipeline.getUuid());
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    executionArgs.setWorkflowVariables(pipelineVariables);

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(workflowExecution.getPipelineExecution()).isNotNull();
    assertThat(workflowExecution.getPipelineExecution().getPipelineStageExecutions().size()).isEqualTo(2);
    List<PipelineStageExecution> pipelineStageExecutions =
        workflowExecution.getPipelineExecution().getPipelineStageExecutions();
    PipelineStageExecution p1 = pipelineStageExecutions.get(0);
    PipelineStageExecution p2 = pipelineStageExecutions.get(1);
    assertThat(p1.getPipelineStageElementId()).isNotEmpty();
    assertThat(p1.getPipelineStageElementId()).isEqualTo(p2.getPipelineStageElementId());
    assertThat(p1.getParallelInfo()).isNotNull();
    assertThat(p1.getParallelInfo().getGroupIndex()).isEqualTo(1);
    assertThat(p2.getParallelInfo()).isNotNull();
    assertThat(p2.getParallelInfo().getGroupIndex()).isEqualTo(1);
  }
}
