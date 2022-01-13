/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.workflowExecution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.states.HttpState.HttpStateKeys;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ExecutionRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;

import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.security.UserGroup;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@Slf4j
public class StepSkipExecutionTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_VARIABLE_NAME = "var_workflow";
  private static final String SWEEPING_OUTPUT_VARIABLE_NAME = "var_so";
  private static final String APPROVAL_VARIABLE_NAME = "var_approval";

  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;

  private Application application;
  private Environment environment;
  private Pipeline pipeline;
  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    OwnerManager.Owners owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    assertThat(service).isNotNull();

    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    assertThat(environment).isNotNull();

    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AWS_SSH_TEST);
    assertThat(infrastructureDefinition).isNotNull();
    log.info("Created basic entities");

    // We now have the app, service, env and infra def fully setup.
    // Find account administrator user group.
    List<UserGroup> userGroupLists = UserGroupRestUtils.getUserGroups(getAccount(), bearerToken);
    String userGroupId = userGroupLists.get(0).getUuid();
    assertThat(userGroupId).isNotNull();
    log.info("User group: {} [{}]", userGroupLists.get(0).getName(), userGroupId);

    List<PhaseStep> phaseSteps1 =
        asList(aPhaseStep(ENABLE_SERVICE)
                   .addStep(getHTTPNode(1))
                   .addStep(getHTTPNode(2))
                   // Test skip all.
                   .withStepSkipStrategies(singletonList(new StepSkipStrategy(StepSkipStrategy.Scope.ALL_STEPS, null,
                       format("${workflow.variables.%s} == 'true'", WORKFLOW_VARIABLE_NAME))))
                   .build(),
            aPhaseStep(VERIFY_SERVICE)
                .addStep(getHTTPNode(3))
                // Test pipeline variables.
                .withStepSkipStrategies(singletonList(new StepSkipStrategy(StepSkipStrategy.Scope.ALL_STEPS, null,
                    format("${%s.variables.%s} == 'true'", SWEEPING_OUTPUT_VARIABLE_NAME, APPROVAL_VARIABLE_NAME))))
                .build());
    phaseSteps1.get(0).setName("PHASE STEP 1");
    phaseSteps1.get(1).setName("PHASE STEP 2");

    GraphNode node1 = getHTTPNode(4);
    GraphNode node2 = getHTTPNode(5);
    GraphNode node3 = getHTTPNode(6);
    GraphNode node4 = getHTTPNode(7);
    List<PhaseStep> phaseSteps2 = singletonList(
        aPhaseStep(ENABLE_SERVICE)
            .addStep(node1)
            .addStep(node2)
            .addStep(node3)
            .addStep(node4)
            // Test specific steps.
            .withStepSkipStrategies(
                asList(new StepSkipStrategy(StepSkipStrategy.Scope.SPECIFIC_STEPS, asList(node1.getId(), node2.getId()),
                           format("${workflow.variables.%s} == 'true'", WORKFLOW_VARIABLE_NAME)),
                    new StepSkipStrategy(StepSkipStrategy.Scope.SPECIFIC_STEPS, singletonList(node3.getId()),
                        format("${workflow.variables.%s} != 'true'", WORKFLOW_VARIABLE_NAME))))
            .build());
    phaseSteps2.get(0).setName("PHASE STEP 1");

    log.info("Creating workflow...");
    Workflow workflow = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .name("Skip Step Workflow - " + generateUuid() + " - " + System.currentTimeMillis())
            .workflowType(WorkflowType.ORCHESTRATION)
            .envId(environment.getUuid())
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withOrchestrationWorkflowType(OrchestrationWorkflowType.MULTI_SERVICE)
                    .withUserVariables(singletonList(
                        aVariable().name(WORKFLOW_VARIABLE_NAME).type(VariableType.TEXT).mandatory(true).build()))
                    // Test pre-deployment steps.
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT)
                            .addStep(getHTTPNode(8))
                            .withStepSkipStrategies(singletonList(new StepSkipStrategy(StepSkipStrategy.Scope.ALL_STEPS,
                                null, format("${workflow.variables.%s} == 'true'", WORKFLOW_VARIABLE_NAME))))
                            .build())
                    // Test post-deployment steps.
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getHTTPNode(9))
                            .withStepSkipStrategies(singletonList(new StepSkipStrategy(StepSkipStrategy.Scope.ALL_STEPS,
                                null, format("${workflow.variables.%s} != 'true'", WORKFLOW_VARIABLE_NAME))))
                            .build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .serviceId(service.getUuid())
                                          .deploymentType(DeploymentType.SSH)
                                          .daemonSet(false)
                                          .infraDefinitionId(infrastructureDefinition.getUuid())
                                          .infraDefinitionName(infrastructureDefinition.getName())
                                          .phaseSteps(phaseSteps1)
                                          .build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .serviceId(service.getUuid())
                                          .deploymentType(DeploymentType.SSH)
                                          .daemonSet(false)
                                          .infraDefinitionId(infrastructureDefinition.getUuid())
                                          .infraDefinitionName(infrastructureDefinition.getName())
                                          .phaseSteps(phaseSteps2)
                                          .build())
                    .build())
            .build());
    assertThat(workflow).isNotNull();
    log.info("Created workflow");

    PipelineStage approvalStage =
        PipelineStage.builder()
            .name(generateUuid())
            .pipelineStageElements(
                singletonList(PipelineStageElement.builder()
                                  .uuid(generateUuid())
                                  .name("Approval")
                                  .type(APPROVAL.name())
                                  .properties(ImmutableMap.of("approvalStateType", "USER_GROUP", "timeoutMillis",
                                      1800000, "userGroups", singletonList(userGroupId), "variables",
                                      singletonList(ImmutableMap.of("name", APPROVAL_VARIABLE_NAME, "value", "true")),
                                      "sweepingOutputName", SWEEPING_OUTPUT_VARIABLE_NAME))
                                  .build()))
            .build();

    PipelineStage workflowStage =
        PipelineStage.builder()
            .name(generateUuid())
            .pipelineStageElements(singletonList(
                PipelineStageElement.builder()
                    .uuid(generateUuid())
                    .name("Multi-Service Workflow")
                    .type(ENV_STATE.name())
                    .properties(ImmutableMap.of("envId", environment.getUuid(), "workflowId", workflow.getUuid()))
                    .workflowVariables(new HashMap<>())
                    .build()))
            .build();

    // Add the pipeline variable using an approval stage.
    List<PipelineStage> pipelineStages = asList(approvalStage, workflowStage);

    log.info("Creating pipeline...");
    pipeline = PipelineRestUtils.createPipeline(application.getAppId(),
        Pipeline.builder()
            .accountId(application.getAccountId())
            .appId(application.getUuid())
            .name("Skip Step Pipeline - " + generateUuid() + " - " + System.currentTimeMillis())
            .pipelineStages(pipelineStages)
            .build(),
        getAccount().getUuid(), bearerToken);
    assertThat(pipeline).isNotNull();
    log.info("Created pipeline");
  }

  @Test
  @Owner(developers = GARVIT, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecutePipelineWithTrueValues() {
    Map<String, String> stateToStatusMap = executePipeline(true, true);
    assertNodeStatus(stateToStatusMap, "SUCCESS", asList(6, 7, 9));
    assertNodeStatus(stateToStatusMap, "SKIPPED", asList(1, 2, 3, 4, 5, 8));
  }

  @Test
  @Owner(developers = GARVIT, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldExecutePipelineWithFalseValues() {
    Map<String, String> stateToStatusMap = executePipeline(false, false);
    assertNodeStatus(stateToStatusMap, "SUCCESS", asList(1, 2, 3, 4, 5, 7, 8));
    assertNodeStatus(stateToStatusMap, "SKIPPED", asList(6, 9));
  }

  private Map<String, String> executePipeline(boolean workflowVariableValue, boolean approvalVariableValue) {
    ExecutionArgs executionArgs =
        getExecutionArgs(pipeline, ImmutableMap.of(WORKFLOW_VARIABLE_NAME, Boolean.toString(workflowVariableValue)));
    // Execute pipeline.
    Map<String, Object> pipelineExecution = ExecutionRestUtils.runPipeline(
        bearerToken, application.getAppId(), environment.getUuid(), pipeline.getUuid(), executionArgs);
    assertThat(pipelineExecution).isNotNull();
    String executionId = (String) pipelineExecution.get("uuid");
    log.info("Started pipeline execution: {}", executionId);

    // Wait for the workflow execution to reach PAUSED state, ie. waiting for approval.
    log.info("Waiting for approval status: {}", executionId);
    awaitForStatus(executionId, singletonList(ExecutionStatus.PAUSED));
    Map<Object, Object> pipelineExecutionMap = getPipelineExecutionMap(executionId);
    assertThat(pipelineExecutionMap).isNotNull();

    String approvalId =
        (String) ((Map<Object, Object>) ((Map<Object, Object>) ((List<Map<Object, Object>>) pipelineExecutionMap.get(
                                                                    "pipelineStageExecutions"))
                                             .get(0))
                      .get("stateExecutionData"))
            .get("approvalId");
    assertThat(approvalId).isNotNull();
    log.info("Got approval: {}", approvalId);

    // Approve the pipeline and set the pipeline variable.
    log.info("Approving pipeline execution: {}, approval: {}", executionId, approvalId);
    ExecutionRestUtils.approvePipeline(bearerToken, getAccount(), application.getUuid(), executionId, approvalId,
        ApprovalDetails.Action.APPROVE,
        singletonList(new NameValuePair(APPROVAL_VARIABLE_NAME, Boolean.toString(approvalVariableValue), null)));
    log.info("Approved pipeline");

    // Wait for the execution to terminate and assert that it is successful.
    log.info("Waiting for pipeline execution completion....");
    awaitForStatus(executionId, asList(ExecutionStatus.SUCCESS, ExecutionStatus.FAILED, ExecutionStatus.REJECTED));
    Map<Object, Object> workflowExecutionMap = getWorkflowExecutionMap(executionId);
    assertThat((String) workflowExecutionMap.get("status")).isEqualTo(ExecutionStatus.SUCCESS.name());
    log.info("Pipeline execution completed");

    String subWorkflowExecutionId =
        (String) ((Map<Object,
                      Object>) ((List<Map<Object,
                                        Object>>) ((Map<Object,
                                                       Object>) ((List<Map<Object,
                                                                         Object>>) ((Map<Object, Object>)
                                                                                        workflowExecutionMap.get(
                                                                                            "pipelineExecution"))
                                                                     .get("pipelineStageExecutions"))
                                                       .get(1))
                                    .get("workflowExecutions"))
                      .get(0))
            .get("uuid");

    // Get the execution node details so that we can verify steps were skipped correctly.
    WorkflowExecution workflowExecution = getExecutionDetails(subWorkflowExecutionId);
    assertThat(workflowExecution).isNotNull();

    // Fetch all the HTTP nodes and their statuses.
    List<GraphNode> nodes = getAllHttpNodes(workflowExecution);
    assertThat(nodes).isNotNull();
    assertThat(nodes.size()).isEqualTo(9);

    return nodes.stream().collect(toMap(GraphNode::getName, GraphNode::getStatus));
  }

  private Map<Object, Object> getWorkflowExecutionMap(String executionId) {
    return ExecutionRestUtils.getWorkflowExecution(bearerToken, getAccount(), application.getAppId(), executionId);
  }

  private Map<Object, Object> getPipelineExecutionMap(String executionId) {
    Map<Object, Object> workflowExecution = getWorkflowExecutionMap(executionId);
    return (Map<Object, Object>) workflowExecution.get("pipelineExecution");
  }

  private WorkflowExecution getExecutionDetails(String executionId) {
    return workflowExecutionService.getExecutionDetails(application.getUuid(), executionId, false, false);
  }

  private void assertNodeStatus(Map<String, String> stateToStatusMap, String status, Collection<Integer> nodes) {
    for (int node : nodes) {
      assertThat(stateToStatusMap.get(format("HTTP %d", node))).isEqualTo(status);
    }
  }

  private List<GraphNode> getAllHttpNodes(WorkflowExecution workflowExecution) {
    List<GraphNode> nodes = new ArrayList<>();
    addAllHttpNodes(nodes, workflowExecution.getExecutionNode());
    return nodes;
  }

  private void addAllHttpNodes(List<GraphNode> nodes, GraphNode start) {
    if (start == null) {
      return;
    }

    if ("HTTP".equals(start.getType())) {
      nodes.add(start);
    }

    if (start.getGroup() != null && EmptyPredicate.isNotEmpty(start.getGroup().getElements())) {
      for (GraphNode el : start.getGroup().getElements()) {
        addAllHttpNodes(nodes, el);
      }
    }

    if (start.getNext() != null) {
      addAllHttpNodes(nodes, start.getNext());
    }
  }

  private void awaitForStatus(String executionId, Collection<ExecutionStatus> statuses) {
    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      Map<Object, Object> pipelineExecution = getPipelineExecutionMap(executionId);
      String status = (String) ((Map<Object, Object>) pipelineExecution).get("status");
      return statuses.contains(ExecutionStatus.valueOf(status));
    });
  }

  private ExecutionArgs getExecutionArgs(Pipeline pipeline, ImmutableMap<String, String> workflowFlowVariables) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    executionArgs.setPipelineId(pipeline.getUuid());
    executionArgs.setNotifyTriggeredUserOnly(false);
    executionArgs.setExecutionCredential(null);
    executionArgs.setExcludeHostsWithSameArtifact(false);
    executionArgs.setWorkflowVariables(workflowFlowVariables);
    return executionArgs;
  }

  private GraphNode getHTTPNode(int index) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name(format("HTTP %d", index))
        .properties(ImmutableMap.<String, Object>builder()
                        .put(HttpStateKeys.url, "http://www.google.com")
                        .put(HttpStateKeys.method, "GET")
                        .build())
        .build();
  }
}
