/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.PageRequest.PageRequestBuilder;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BlueGreenOrchestrationWorkflow.BlueGreenOrchestrationWorkflowBuilder.aBlueGreenOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.RollingOrchestrationWorkflow.RollingOrchestrationWorkflowBuilder.aRollingOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.DEPLOY;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.DEPLOY_CONTAINERS;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.PCF_RESIZE;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.PCF_ROLLBACK;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.PCF_SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.ROLLBACK_PREFIX;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.SHELL_SCRIPT;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.VERIFY_SERVICE;
import static software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.AWS_AMI_ROLLBACK_SWITCH_ROUTES;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.ECS_RUN_TASK;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.WorkflowType;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.exception.InvalidRequestException;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.TemplateGenerator;

import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import com.google.api.client.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDP)
@Singleton
public class WorkflowUtils {
  private static final long TEST_TIMEOUT_IN_MINUTES = 8;
  static final String SETUP_CONTAINER_CONSTANT = "Setup Container";
  static final String PRE_DEPLOYMENT_CONSTANT = "Pre-Deployment";
  static final String ECS_DAEMON_SERVICE_SETUP_NAME = "ECS Daemon Service Setup";
  static final String POST_DEPLOYMENT_CONSTANT = "Post-Deployment";
  static final String WRAP_UP_CONSTANT = "Wrap Up";
  static final String ECS_SERVICE_SETUP_CONSTANT = "ECS Service Setup";
  static final String ECS_RUN_TASK_SETUP_CONSTANT = "ECS Run Task Setup";
  static final String UPGRADE_CONTAINERS_CONSTANT = "Upgrade Containers";
  static final String DEPLOY_CONTAINERS_CONSTANT = "Deploy Containers";
  static final String ROLLBACK_SERVICE_CONSTANT = "Rollback Service";
  static final String DEPLOY_SERVICE_CONSTANT = "Deploy Service";
  static final String SELECT_NODES_CONSTANT = "Select Node";
  static final String APPROVAL_CONSTANT = "Approval";

  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private TemplateGenerator templateGenerator;
  @Inject private StateExecutionService stateExecutionService;

  public WorkflowExecution checkForWorkflowSuccess(WorkflowExecution workflowExecution) {
    WorkflowExecution finalWorkflowExecution = awaitAndFetchFinalWorkflowExecution(workflowExecution);
    if (finalWorkflowExecution.getStatus() != ExecutionStatus.SUCCESS) {
      throw new InvalidRequestException(
          "workflow execution did not succeed. Final status: " + finalWorkflowExecution.getStatus());
    }
    return finalWorkflowExecution;
  }

  public void validateWorkflowStatus(WorkflowExecution workflowExecution, ExecutionStatus expectedStatus) {
    WorkflowExecution finalWorkflowExecution = awaitAndFetchFinalWorkflowExecution(workflowExecution);
    if (finalWorkflowExecution.getStatus() != expectedStatus) {
      throw new InvalidRequestException("workflow execution did not complete with expected status. Final status: "
          + finalWorkflowExecution.getStatus() + " Expected status: " + expectedStatus);
    }
  }

  private WorkflowExecution awaitAndFetchFinalWorkflowExecution(WorkflowExecution workflowExecution) {
    Awaitility.await().atMost(TEST_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus status =
          workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid())
              .getStatus();
      return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED;
    });
    return workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid());
  }

  public static Workflow buildCanaryWorkflowPostDeploymentStep(String name, String envId, GraphNode graphNode) {
    return aWorkflow()
        .name(name)
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).addStep(graphNode).build())
                                   .build())
        .build();
  }

  public Workflow buildCanaryWorkflowPostDeploymentStep(String name, String envId) {
    return aWorkflow()
        .name(name)
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  public Workflow getCanaryK8sWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    return aWorkflow()
        .name(name + System.currentTimeMillis())
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .serviceId(service.getUuid())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .sample(true)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
        .build();
  }

  public Workflow getEcsEc2TypeCanaryWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    phaseSteps.add(ecsContainerSetupPhaseStep());
    phaseSteps.add(ecsContainerDeployPhaseStep());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflow()
        .name(name + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.ECS)
                                      .daemonSet(false)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .phaseSteps(phaseSteps)
                                      .build())
                .build())
        .build();
  }

  public PhaseStep ecsContainerDeployPhaseStep() {
    return aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(ECS_SERVICE_DEPLOY.name())
                     .name(UPGRADE_CONTAINERS_CONSTANT)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("instanceUnitType", "PERCENTAGE")
                                     .put("instanceCount", 100)
                                     .put("downsizeInstanceUnitType", "PERCENTAGE")
                                     .put("downsizeInstanceCount", 0)
                                     .build())
                     .build())
        .build();
  }

  public PhaseStep ecsContainerSetupPhaseStep() {
    return aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(ECS_SERVICE_SETUP.name())
                     .name(ECS_SERVICE_SETUP_CONSTANT)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("fixedInstances", "1")
                                     .put("useLoadBalancer", false)
                                     .put("ecsServiceName", "${app.name}__${service.name}__BASIC")
                                     .put("desiredInstanceCount", "fixedInstances")
                                     .put("resizeStrategy", ResizeStrategy.DOWNSIZE_OLD_FIRST)
                                     .put("serviceSteadyStateTimeout", 10)
                                     .build())
                     .build())
        .build();
  }

  public PhaseStep ecsRunTaskPhaseStep() {
    return aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(ECS_RUN_TASK.name())
                     .name(ECS_RUN_TASK_SETUP_CONSTANT)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("addTaskDefinition", "inline")
                                     .put("inlineTaskDefintion", "")
                                     .put("serviceSteadyStateTimeout", 10)
                                     .build())
                     .build())
        .build();
  }

  public Workflow createCanarySshWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    name = Joiner.on(StringUtils.EMPTY).join(name, System.currentTimeMillis());

    List<PhaseStep> phaseSteps = Lists.newArrayList();
    phaseSteps.add(aPhaseStep(PhaseStepType.SELECT_NODE, PhaseStepType.SELECT_NODE.name())
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_NODE_SELECT.name())
                                    .name(SELECT_NODES_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceUnitType", "PERCENTAGE")
                                                    .put("instanceCount", 100)
                                                    .put("specificHosts", false)
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DISABLE_SERVICE, PhaseStepType.DISABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, PhaseStepType.DEPLOY_SERVICE.name())
                       .addStep(getInstallStep())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, PhaseStepType.VERIFY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());
    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                .withWorkflowPhases(Collections.singletonList(aWorkflowPhase()
                                                                  .serviceId(service.getUuid())
                                                                  .infraDefinitionId(infrastructureDefinition.getUuid())
                                                                  .phaseSteps(phaseSteps)
                                                                  .build()))
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public Workflow createWinRMWorkflow(String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    name = Joiner.on(StringUtils.EMPTY).join(name, System.currentTimeMillis());
    List<PhaseStep> phaseSteps = Lists.newArrayList();
    phaseSteps.add(aPhaseStep(PhaseStepType.SELECT_NODE, PhaseStepType.SELECT_NODE.name())
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_NODE_SELECT.name())
                                    .name(SELECT_NODES_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceUnitType", "PERCENTAGE")
                                                    .put("instanceCount", 100)
                                                    .put("specificHosts", false)
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DISABLE_SERVICE, PhaseStepType.DISABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, PhaseStepType.DEPLOY_SERVICE.name())
                       .addStep(getInstallStep())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, PhaseStepType.VERIFY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());
    WorkflowPhase phase = aWorkflowPhase()
                              .uuid(generateUuid())
                              .serviceId(service.getUuid())
                              .infraDefinitionId(infrastructureDefinition.getUuid())
                              .phaseSteps(phaseSteps)
                              .build();
    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    rollbackMap.put(phase.getUuid(), obtainRollbackPhase(phase, getInstallStep()));
    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(Collections.singletonList(phase))
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  public WorkflowPhase obtainRollbackPhase(WorkflowPhase workflowPhase, GraphNode graphNode) {
    WorkflowPhaseBuilder rollbackPhaseBuilder = aWorkflowPhase()
                                                    .name(ROLLBACK_PREFIX + workflowPhase.getName())
                                                    .deploymentType(workflowPhase.getDeploymentType())
                                                    .rollback(true)
                                                    .phaseNameForRollback(workflowPhase.getName())
                                                    .serviceId(workflowPhase.getServiceId())
                                                    .computeProviderId(workflowPhase.getComputeProviderId())
                                                    .infraMappingId(workflowPhase.getInfraMappingId())
                                                    .infraMappingName(workflowPhase.getInfraMappingName())
                                                    .infraDefinitionId(workflowPhase.getInfraDefinitionId());

    return rollbackPhaseBuilder
        .phaseSteps(asList(aPhaseStep(PhaseStepType.DISABLE_SERVICE, PhaseStepType.DISABLE_SERVICE.name())
                               .withPhaseStepNameForRollback(PhaseStepType.ENABLE_SERVICE.name())
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.DEPLOY_SERVICE, PhaseStepType.DEPLOY_SERVICE.name())
                .addStep(graphNode)
                .withPhaseStepNameForRollback(PhaseStepType.DEPLOY_SERVICE.name())
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name())
                .withPhaseStepNameForRollback(PhaseStepType.DISABLE_SERVICE.name())
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(PhaseStepType.DEPLOY_SERVICE.name())
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, PhaseStepType.WRAP_UP.name()).withRollback(true).build()))
        .build();
  }

  private GraphNode getInstallStep() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(COMMAND.name())
        .name("Install")
        .properties(ImmutableMap.<String, Object>builder().put("commandName", "Install").build())
        .rollback(false)
        .build();
  }

  public Workflow createPcfWorkflow(String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    WorkflowPhase workflowPhase = obtainWorkflowPhasePcf(service, infrastructureDefinition);
    WorkflowPhase rollbackPhase = obtainRollbackPhasePcf(workflowPhase);
    return createPcfWorkflow(name, service, infrastructureDefinition, workflowPhase, rollbackPhase);
  }

  public Workflow createPcfCommandWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    WorkflowPhase workflowPhase = obtainWorkflowPhasePcfCommand(service, infrastructureDefinition);
    WorkflowPhase rollbackPhase = obtainRollbackPhasePcfCommand(workflowPhase);
    return createPcfWorkflow(name, service, infrastructureDefinition, workflowPhase, rollbackPhase);
  }

  public Workflow createLinkedPcfCommandWorkflow(Randomizer.Seed seed, OwnerManager.Owners owners, String name,
      Service service, InfrastructureDefinition infrastructureDefinition) {
    WorkflowPhase workflowPhase = obtainWorkflowPhasePcfCommandLinked(seed, owners, service, infrastructureDefinition);
    WorkflowPhase rollbackPhase = obtainRollbackPhasePcfCommand(workflowPhase);
    return createPcfWorkflow(name, service, infrastructureDefinition, workflowPhase, rollbackPhase);
  }

  private WorkflowPhase obtainRollbackPhasePcfCommand(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .name(ROLLBACK_PREFIX + workflowPhase.getName())
        .deploymentType(workflowPhase.getDeploymentType())
        .rollback(true)
        .phaseNameForRollback(workflowPhase.getName())
        .serviceId(workflowPhase.getServiceId())
        .computeProviderId(workflowPhase.getComputeProviderId())
        .infraDefinitionId(workflowPhase.getInfraDefinitionId())
        .phaseSteps(asList(aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                               .withPhaseStepNameForRollback(DEPLOY)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP_CONSTANT).withRollback(true).build()))
        .build();
  }

  private WorkflowPhase obtainWorkflowPhasePcfCommand(
      Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    Map<String, Object> pcfCommandProperties = new HashMap<>();
    pcfCommandProperties.put("scriptString",
        "cf apps \n cat ${service.manifest}/manifest.yml \n cat ${service.manifest.repoRoot}/pcf-app1/vars.yml");

    phaseSteps.add(aPhaseStep(PhaseStepType.PCF_SETUP, SETUP)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_PLUGIN.name())
                                    .name(WorkflowServiceHelper.PCF_PLUGIN)
                                    .properties(pcfCommandProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE).build());

    return aWorkflowPhase()
        .uuid(generateUuid())
        .serviceId(service.getUuid())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .phaseSteps(phaseSteps)
        .build();
  }

  private WorkflowPhase obtainWorkflowPhasePcfCommandLinked(Randomizer.Seed seed, OwnerManager.Owners owners,
      Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    Template pcfCommandTemplate =
        templateGenerator.ensurePredefined(seed, owners, TemplateGenerator.Templates.PCF_COMMAND_TEMPLATE_1);
    PcfCommandTemplate templateObject = (PcfCommandTemplate) pcfCommandTemplate.getTemplateObject();
    Map<String, Object> properties = new HashMap<>();
    properties.put("scriptString", templateObject.getScriptString());
    properties.put("timeoutIntervalInMinutes", templateObject.getTimeoutIntervalInMinutes());

    phaseSteps.add(aPhaseStep(PhaseStepType.PCF_SETUP, SETUP)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_PLUGIN.name())
                                    .name(WorkflowServiceHelper.PCF_PLUGIN)
                                    .properties(properties)
                                    .templateVariables(pcfCommandTemplate.getVariables())
                                    .templateUuid(pcfCommandTemplate.getUuid())
                                    .templateVersion("latest")
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE).build());

    return aWorkflowPhase()
        .uuid(generateUuid())
        .serviceId(service.getUuid())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .phaseSteps(phaseSteps)
        .build();
  }

  public Workflow createPcfWorkflow(String name, Service service, InfrastructureDefinition infrastructureDefinition,
      WorkflowPhase workflowPhase, WorkflowPhase rollbackPhase) {
    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    rollbackMap.put(workflowPhase.getUuid(), rollbackPhase);
    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(Collections.singletonList(workflowPhase))
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  private WorkflowPhase obtainWorkflowPhasePcf(Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    Map<String, Object> defaultProperties = new HashMap<>();
    defaultProperties.put("blueGreen", false);
    defaultProperties.put("isWorkflowV2", true);
    defaultProperties.put("resizeStrategy", "DOWNSIZE_OLD_FIRST");

    phaseSteps.add(aPhaseStep(PhaseStepType.PCF_SETUP, SETUP)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_SETUP.name())
                                    .name(PCF_SETUP)
                                    .properties(defaultProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.PCF_RESIZE.name())
                                    .name(PCF_RESIZE)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceCount", 1)
                                                    .put("instanceUnitType", InstanceUnitType.COUNT)
                                                    .put("downsizeInstanceCount", 1)
                                                    .put("downsizeInstanceUnitType", InstanceUnitType.COUNT)
                                                    .build())
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE).build());

    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflowPhase()
        .uuid(generateUuid())
        .serviceId(service.getUuid())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .phaseSteps(phaseSteps)
        .build();
  }

  private WorkflowPhase obtainRollbackPhasePcf(WorkflowPhase workflowPhase) {
    WorkflowPhaseBuilder rollbackPhaseBuilder = aWorkflowPhase()
                                                    .name(ROLLBACK_PREFIX + workflowPhase.getName())
                                                    .deploymentType(workflowPhase.getDeploymentType())
                                                    .rollback(true)
                                                    .phaseNameForRollback(workflowPhase.getName())
                                                    .serviceId(workflowPhase.getServiceId())
                                                    .computeProviderId(workflowPhase.getComputeProviderId())
                                                    .infraDefinitionId(workflowPhase.getInfraDefinitionId());

    return rollbackPhaseBuilder
        .phaseSteps(asList(aPhaseStep(PhaseStepType.PCF_RESIZE, DEPLOY)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(StateType.PCF_ROLLBACK.name())
                                            .name(PCF_ROLLBACK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            // When we rolling back the verification steps
            // the same criteria to run if deployment is needed should be used
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP_CONSTANT).withRollback(true).build()))
        .build();
  }

  public Workflow getRollingK8sTemplatizedWorkflow(
      String name, Service service, InfrastructureDefinition defaultInfrastructureDefinition) {
    TemplateExpression templateExpressionForEnv = getTemplateExpressionsForEnv();
    TemplateExpression templateExpressionForInfraDefinition =
        getTemplateExpressionsForInfraDefinition("${InfraDefinition_Kubernetes}");

    Workflow workflow = aWorkflow()
                            .name(name + System.currentTimeMillis())
                            .appId(service.getAppId())
                            .envId(defaultInfrastructureDefinition.getEnvId())
                            .infraDefinitionId(defaultInfrastructureDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aRollingOrchestrationWorkflow().build())
                            .templatized(true)
                            .templateExpressions(asList(templateExpressionForEnv, templateExpressionForInfraDefinition))
                            .build();
    return workflow;
  }

  public Workflow getRollingK8sWorkflow(
      String name, Service service, InfrastructureDefinition defaultInfrastructureDefinition) {
    Workflow workflow = aWorkflow()
                            .name(name + System.currentTimeMillis())
                            .appId(service.getAppId())
                            .envId(defaultInfrastructureDefinition.getEnvId())
                            .infraDefinitionId(defaultInfrastructureDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aRollingOrchestrationWorkflow().build())
                            .build();
    return workflow;
  }

  public Workflow createSpotinstCanaryWorkflowWithVerifyStep(
      String name, Service service, InfrastructureDefinition infrastructureDefinition, String elkConfigId) {
    List<PhaseStep> canaryPhaseSteps = new ArrayList<>();

    canaryPhaseSteps.add(
        aPhaseStep(PhaseStepType.SPOTINST_SETUP, "Elastigroup Setup")
            .addStep(GraphNode.builder()
                         .id(generateUuid())
                         .type(StateType.SPOTINST_SETUP.name())
                         .name("Elastigroup Setup")
                         .properties(ImmutableMap.<String, Object>builder()
                                         .put("minInstances", 0)
                                         .put("maxInstances", 2)
                                         .put("targetInstances", 2)
                                         .put("elastiGroupNamePrefix", "${app.name}_${service.name}_${env.name}")
                                         .put("timeoutIntervalInMin", 20)
                                         .put("blueGreen", false)
                                         .put("useCurrentRunningCount", false)
                                         .put("resizeStrategy", "RESIZE_NEW_FIRST")
                                         .put("useLoadBalancer", false)
                                         .build())
                         .build())
            .build());

    canaryPhaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_DEPLOY, "Elastigroup Deploy")
                             .addStep(GraphNode.builder()
                                          .id(generateUuid())
                                          .type(StateType.SPOTINST_DEPLOY.name())
                                          .name("Elastigroup deploy")
                                          .properties(ImmutableMap.<String, Object>builder()
                                                          .put("instanceCount", 1)
                                                          .put("instanceUnitType", "COUNT")
                                                          .put("downsizeInstanceUnitType", "PERCENTAGE")
                                                          .build())
                                          .build())
                             .build());

    canaryPhaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, "Verify Canary")
                             .addStep(GraphNode.builder()
                                          .id(generateUuid())
                                          .type(StateType.ELK.name())
                                          .name("ELK")
                                          .properties(ImmutableMap.<String, Object>builder()
                                                          .put("analysisServerConfigId", elkConfigId)
                                                          .put("indices", "qa-integration-test-*")
                                                          .put("timestampField", "@timestamp")
                                                          .put("messageField", "message")
                                                          .put("timestampFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
                                                          .put("queryType", "MATCH")
                                                          .put("query", "*exception*")
                                                          .put("timeDuration", 2)
                                                          .put("comparisonStrategy", "COMPARE_WITH_CURRENT")
                                                          .put("initialAnalysisDelay", "2m")
                                                          .put("hostnameField", "hostname")
                                                          .build())
                                          .build())
                             .build());

    List<PhaseStep> primaryPhaseSteps = new ArrayList<>();

    primaryPhaseSteps.add(aPhaseStep(PhaseStepType.SPOTINST_DEPLOY, "Elastigroup Deploy")
                              .addStep(GraphNode.builder()
                                           .id(generateUuid())
                                           .type(StateType.SPOTINST_DEPLOY.name())
                                           .name("Elastigroup deploy")
                                           .properties(ImmutableMap.<String, Object>builder()
                                                           .put("instanceCount", 100)
                                                           .put("instanceUnitType", "PERCENTAGE")
                                                           .put("downsizeInstanceUnitType", "PERCENTAGE")
                                                           .build())
                                           .build())
                              .build());

    return aWorkflow()
        .name(name + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .addWorkflowPhase(aWorkflowPhase()
                                                         .serviceId(service.getUuid())
                                                         .deploymentType(DeploymentType.SPOTINST)
                                                         .daemonSet(false)
                                                         .infraDefinitionId(infrastructureDefinition.getUuid())
                                                         .infraDefinitionName(infrastructureDefinition.getName())
                                                         .phaseSteps(canaryPhaseSteps)
                                                         .build())
                                   .addWorkflowPhase(aWorkflowPhase()
                                                         .serviceId(service.getUuid())
                                                         .deploymentType(DeploymentType.SPOTINST)
                                                         .daemonSet(false)
                                                         .infraDefinitionId(infrastructureDefinition.getUuid())
                                                         .infraDefinitionName(infrastructureDefinition.getName())
                                                         .phaseSteps(primaryPhaseSteps)
                                                         .build())
                                   .build())
        .build();
  }

  public Workflow createAwsAmiBGWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(aPhaseStep(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP,
        " Setup AutoScaling "
            + "Group")
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.AWS_AMI_SERVICE_SETUP.toString())
                                    .name("Ami Setup")
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("autoScalingGroupName",
                                                        "${app.name}_${service"
                                                            + ".name}_${env.name}")
                                                    .put("maxInstances", 1)
                                                    .put("desiredInstances", 1)
                                                    .put("resizeStrategy", "RESIZE_NEW_FIRST")
                                                    .put("minInstances", 0)
                                                    .put("autoScalingSteadyStateTimeout", 10)
                                                    .put("useCurrentRunningCount", false)
                                                    .build())

                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, "Deploy")
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .name("deploy")
                                    .type(StateType.AWS_AMI_SERVICE_DEPLOY.toString())
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceCount", 1)
                                                    .put("instanceUnitType", "COUNT")
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, "Verify Service").build());
    phaseSteps.add(
        aPhaseStep(PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, "Swap Routes")
            .addStep(GraphNode.builder()
                         .id(generateUuid())
                         .name("swap asg")
                         .type(StateType.AWS_AMI_SWITCH_ROUTES.toString())
                         .properties(ImmutableMap.<String, Object>builder().put("downsizeOldAsg", true).build())
                         .build())
            .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflow()
        .name(name + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(aBlueGreenOrchestrationWorkflow()
                                   .addWorkflowPhase(aWorkflowPhase()
                                                         .serviceId(service.getUuid())
                                                         .deploymentType(DeploymentType.AMI)
                                                         .daemonSet(false)
                                                         .infraDefinitionId(infrastructureDefinition.getUuid())
                                                         .infraDefinitionName(infrastructureDefinition.getName())
                                                         .phaseSteps(phaseSteps)
                                                         .build())
                                   .build())
        .build();
  }

  public Workflow createAwsAmiBGRollbackWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(aPhaseStep(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP,
        " Setup AutoScaling "
            + "Group")
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.AWS_AMI_SERVICE_SETUP.toString())
                                    .name("Ami Setup")
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("autoScalingGroupName",
                                                        "${app.name}_${service"
                                                            + ".name}_${env.name}")
                                                    .put("maxInstances", 1)
                                                    .put("desiredInstances", 1)
                                                    .put("resizeStrategy", "RESIZE_NEW_FIRST")
                                                    .put("minInstances", 0)
                                                    .put("autoScalingSteadyStateTimeout", 10)
                                                    .put("useCurrentRunningCount", false)
                                                    .build())

                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, "Deploy")
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .name("deploy")
                                    .type(StateType.AWS_AMI_SERVICE_DEPLOY.toString())
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceCount", 1)
                                                    .put("instanceUnitType", "COUNT")
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, "Verify Service").build());
    phaseSteps.add(
        aPhaseStep(PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, "Swap Routes")
            .addStep(GraphNode.builder()
                         .id(generateUuid())
                         .name("swap asg")
                         .type(StateType.AWS_AMI_SWITCH_ROUTES.toString())
                         .properties(ImmutableMap.<String, Object>builder().put("downsizeOldAsg", true).build())
                         .build())
            .build());

    List<String> userGroups = Collections.singletonList("uK63L5CVSAa1-BkC4rXoRg");
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(APPROVAL.name())
                                    .name(APPROVAL_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("timeoutMillis", 60000)
                                                    .put("approvalStateType", "USER_GROUP")
                                                    .put("userGroups", userGroups)
                                                    .build())
                                    .build())
                       .build());

    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.AMI)
                                      .daemonSet(false)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .phaseSteps(phaseSteps)
                                      .build();

    List<PhaseStep> rollbackPhaseStep = new ArrayList<>();
    rollbackPhaseStep.add(aPhaseStep(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, ROLLBACK_SERVICE_CONSTANT)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .withPhaseStepNameForRollback(DEPLOY_SERVICE_CONSTANT)
                              .addStep(GraphNode.builder()
                                           .id(generateUuid())
                                           .name("Rollback Autoscaling Group Route")
                                           .type(AWS_AMI_ROLLBACK_SWITCH_ROUTES.name())
                                           .rollback(true)
                                           .origin(true)
                                           .properties(ImmutableMap.<String, Object>builder().build())
                                           .build())
                              .build());
    rollbackPhaseStep.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).withRollback(true).build());

    Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    workflowPhaseIdMap.put(workflowPhase.getUuid(),
        aWorkflowPhase()
            .rollback(true)
            .phaseSteps(rollbackPhaseStep)
            .serviceId(service.getUuid())
            .deploymentType(DeploymentType.AMI)
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .build());

    return aWorkflow()
        .name(name + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(aBlueGreenOrchestrationWorkflow()
                                   .addWorkflowPhase(workflowPhase)
                                   .withRollbackWorkflowPhaseIdMap(workflowPhaseIdMap)
                                   .build())
        .build();
  }

  public Workflow createBasicWorkflow(String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    Workflow workflow = aWorkflow()
                            .name(name + System.currentTimeMillis())
                            .appId(service.getAppId())
                            .envId(infrastructureDefinition.getEnvId())
                            .infraDefinitionId(infrastructureDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();
    return workflow;
  }

  public Workflow createBasicWorkflowWithShellScript(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    List<PhaseStep> phaseSteps = Lists.newArrayList();
    phaseSteps.add(aPhaseStep(PhaseStepType.SELECT_NODE, PhaseStepType.SELECT_NODE.name())
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_NODE_SELECT.name())
                                    .name(SELECT_NODES_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceUnitType", "PERCENTAGE")
                                                    .put("instanceCount", 100)
                                                    .put("specificHosts", false)
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DISABLE_SERVICE, PhaseStepType.DISABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, PhaseStepType.DEPLOY_SERVICE.name())
                       .addStep(GraphNode.builder()
                                    .name("shell_script_" + System.currentTimeMillis())
                                    .type(StateType.SHELL_SCRIPT.toString())
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("scriptType", "BASH")
                                                    .put("scriptString", "echo ${artifact.buildNo}")
                                                    .put("executeOnDelegate", "true")
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, PhaseStepType.VERIFY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());
    Workflow workflow =
        aWorkflow()
            .name(name + System.currentTimeMillis())
            .appId(service.getAppId())
            .envId(infrastructureDefinition.getEnvId())
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .serviceId(service.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                    .withWorkflowPhases(asList(aWorkflowPhase()
                                                   .name("Phase1")
                                                   .serviceId(service.getUuid())
                                                   .deploymentType(SSH)
                                                   .phaseSteps(phaseSteps)
                                                   .infraDefinitionId(infrastructureDefinition.getUuid())
                                                   .build()))
                    .build())
            .build();
    return workflow;
  }

  public Workflow createRollingWorkflow(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    Workflow workflow = aWorkflow()
                            .name(name + System.currentTimeMillis())
                            .appId(service.getAppId())
                            .envId(infrastructureDefinition.getEnvId())
                            .infraDefinitionId(infrastructureDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aRollingOrchestrationWorkflow().build())
                            .build();
    return workflow;
  }

  public Workflow createRollingWorkflowInfraDefinition(
      String name, Service service, InfrastructureDefinition infrastructureDefinition) {
    Workflow workflow = aWorkflow()
                            .name(name + System.currentTimeMillis())
                            .appId(service.getAppId())
                            .envId(infrastructureDefinition.getEnvId())
                            .infraDefinitionId(infrastructureDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aRollingOrchestrationWorkflow().build())
                            .build();
    return workflow;
  }

  // Unique name of the workflow is ensured here
  public Workflow createBuildWorkflow(@NotEmpty String name, String appId, @NotEmpty List<String> artifactStreamIds) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    List<GraphNode> steps = new ArrayList<>();
    for (String artifactStreamId : artifactStreamIds) {
      steps.add(
          GraphNode.builder()
              .name("collect-artifact-" + artifactStreamId)
              .type(StateType.ARTIFACT_COLLECTION.toString())
              .properties(ImmutableMap.<String, Object>builder().put("artifactStreamId", artifactStreamId).build())
              .build());
    }
    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS.toString()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT, PhaseStepType.COLLECT_ARTIFACT.toString())
                       .addAllSteps(steps)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    Workflow buildWorkflow =
        aWorkflow()
            .name(name)
            .appId(appId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBuildOrchestrationWorkflow()
                                       .withWorkflowPhases(asList(aWorkflowPhase().phaseSteps(phaseSteps).build()))
                                       .build())
            .build();
    return buildWorkflow;
  }

  // Unique name of the workflow is ensured here
  public Workflow createWorkflowWithShellScriptCommand(
      @NotEmpty String name, String appId, @NotEmpty String scriptType, @NotEmpty String script) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    List<GraphNode> steps = new ArrayList<>();
    steps.add(GraphNode.builder()
                  .name("shell_script_" + System.currentTimeMillis())
                  .type(StateType.SHELL_SCRIPT.toString())
                  .properties(ImmutableMap.<String, Object>builder()
                                  .put("scriptType", scriptType)
                                  .put("scriptString", script)
                                  .put("executeOnDelegate", "true")
                                  .build())
                  .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS.toString()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT, PhaseStepType.COLLECT_ARTIFACT.toString())
                       .addAllSteps(steps)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    Workflow buildWorkflow =
        aWorkflow()
            .name(name + System.currentTimeMillis())
            .appId(appId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBuildOrchestrationWorkflow()
                                       .withWorkflowPhases(asList(aWorkflowPhase().phaseSteps(phaseSteps).build()))
                                       .build())
            .build();
    return buildWorkflow;
  }

  public Workflow createWorkflowWithShellScriptAndDelegateSelector(
      @NotEmpty String name, String appId, @NotEmpty String scriptType, @NotEmpty String script, String[] tags) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    List<GraphNode> steps = new ArrayList<>();
    steps.add(GraphNode.builder()
                  .name("shell_script_" + System.currentTimeMillis())
                  .type(StateType.SHELL_SCRIPT.toString())
                  .properties(ImmutableMap.<String, Object>builder()
                                  .put("scriptType", scriptType)
                                  .put("scriptString", script)
                                  .put("executeOnDelegate", "true")
                                  .put("tags", tags)
                                  .build())
                  .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS.toString()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT, PhaseStepType.COLLECT_ARTIFACT.toString())
                       .addAllSteps(steps)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    Workflow buildWorkflow =
        aWorkflow()
            .name(name + System.currentTimeMillis())
            .appId(appId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBuildOrchestrationWorkflow()
                                       .withWorkflowPhases(asList(aWorkflowPhase().phaseSteps(phaseSteps).build()))
                                       .build())
            .build();
    return buildWorkflow;
  }

  public Workflow createMultiPhaseSshWorkflowWithNoNodePhase(
      String name, Service service, InfrastructureDefinition infrastructureDefinition, boolean withShellScript) {
    List<PhaseStep> phaseSteps = createSshSteps();
    List<WorkflowPhase> workflowPhases =
        new ArrayList<>(asList(aWorkflowPhase()
                                   .serviceId(service.getUuid())
                                   .infraDefinitionId(infrastructureDefinition.getUuid())
                                   .phaseSteps(phaseSteps)
                                   .build(),
            aWorkflowPhase()
                .serviceId(service.getUuid())
                .infraDefinitionId(infrastructureDefinition.getUuid())
                .phaseSteps(phaseSteps)
                .build(),
            aWorkflowPhase()
                .serviceId(service.getUuid())
                .infraDefinitionId(infrastructureDefinition.getUuid())
                .phaseSteps(phaseSteps)
                .build()));

    if (withShellScript) {
      workflowPhases.add(aWorkflowPhase()
                             .serviceId(service.getUuid())
                             .infraDefinitionId(infrastructureDefinition.getUuid())
                             .phaseSteps(Collections.singletonList(
                                 aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name())
                                     .addStep(WorkflowUtils.getShellScriptStep("exit 1"))
                                     .build()))
                             .build());
    }

    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    for (WorkflowPhase workflowPhase : workflowPhases) {
      rollbackMap.put(
          workflowPhase.getUuid(), obtainRollbackPhase(workflowPhase, WorkflowUtils.getShellScriptStep("exit 0")));
    }

    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(workflowPhases)
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  public Workflow createRollingSshWorkflowWithNoNodePhase(
      String name, Service service, InfrastructureDefinition infrastructureDefinition, boolean withShellScript) {
    List<PhaseStep> phaseSteps = createSshSteps();
    if (withShellScript) {
      phaseSteps.get(0).getSteps().add(WorkflowUtils.getShellScriptStep("exit 1"));
    }
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .phaseSteps(phaseSteps)
                                      .build();

    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    rollbackMap.put(
        workflowPhase.getUuid(), obtainRollbackPhase(workflowPhase, WorkflowUtils.getShellScriptStep("exit 0")));

    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aRollingOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(asList(workflowPhase))
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  public Workflow createBasicSshWorkflowWithNoNodePhase(
      String name, Service service, InfrastructureDefinition infrastructureDefinition, boolean withShellScript) {
    List<PhaseStep> phaseSteps = createSshSteps();
    if (withShellScript) {
      phaseSteps.get(0).getSteps().add(WorkflowUtils.getShellScriptStep("exit 1"));
    }
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .phaseSteps(phaseSteps)
                                      .build();

    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    rollbackMap.put(
        workflowPhase.getUuid(), obtainRollbackPhase(workflowPhase, WorkflowUtils.getShellScriptStep("exit 0")));

    return aWorkflow()
        .name(name)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(asList(workflowPhase))
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }

  private List<PhaseStep> createSshSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(aPhaseStep(PhaseStepType.SELECT_NODE, PhaseStepType.SELECT_NODE.name())
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(AWS_NODE_SELECT.name())
                                    .name(SELECT_NODES_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceUnitType", "PERCENTAGE")
                                                    .put("instanceCount", 100)
                                                    .put("specificHosts", false)
                                                    .put("excludeSelectedHostsFromFuturePhases", true)
                                                    .build())
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(PhaseStepType.DISABLE_SERVICE, PhaseStepType.DISABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, PhaseStepType.VERIFY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());
    return phaseSteps;
  }

  public static TemplateExpression getTemplateExpressionsForEnv() {
    Map<String, Object> metaData =
        ImmutableMap.<String, Object>builder().put("entityType", EntityType.ENVIRONMENT.name()).build();
    return TemplateExpression.builder()
        .fieldName("envId")
        .expression("${Environment}")
        .mandatory(true)
        .expressionAllowed(false)
        .metadata(metaData)
        .build();
  }

  public static TemplateExpression getTemplateExpressionsForService() {
    Map<String, Object> metaData =
        ImmutableMap.<String, Object>builder().put("entityType", EntityType.SERVICE.name()).build();
    return TemplateExpression.builder()
        .fieldName("serviceId")
        .expression("${Service}")
        .mandatory(true)
        .expressionAllowed(false)
        .metadata(metaData)
        .build();
  }

  public static GraphNode getShellScriptStep(String script) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(SHELL_SCRIPT)
        .type(StateType.SHELL_SCRIPT.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("scriptType", "BASH")
                        .put("scriptString", script)
                        .put("timeoutMillis", 60000)
                        .put("executeOnDelegate", true)
                        .build())
        .build();
  }

  public static TemplateExpression getTemplateExpressionsForInfraDefinition(String expression) {
    Map<String, Object> metaData =
        ImmutableMap.<String, Object>builder().put("entityType", EntityType.INFRASTRUCTURE_DEFINITION.name()).build();
    return TemplateExpression.builder()
        .fieldName("infraDefinitionId")
        .expression(expression)
        .mandatory(true)
        .expressionAllowed(false)
        .metadata(metaData)
        .build();
  }

  public void assertRollbackInWorkflowExecution(WorkflowExecution workflowExecution) {
    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    PageRequest<StateExecutionInstance> stateExecutionInstancePageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter(StateExecutionInstanceKeys.executionUuid, SearchFilter.Operator.EQ, workflowExecution.getUuid())
            .addFilter(StateExecutionInstanceKeys.rollback, SearchFilter.Operator.EQ, true)
            .addFilter(StateExecutionInstanceKeys.stateType, SearchFilter.Operator.EQ, "PHASE")
            .build();

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse =
        stateExecutionService.list(stateExecutionInstancePageRequest);

    List<StateExecutionInstance> stateExecutionInstances = stateExecutionInstancePageResponse.getResponse();
    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      assertThat(stateExecutionInstance.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    }
  }
}
