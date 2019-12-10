package io.harness.functional;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BlueGreenOrchestrationWorkflow.BlueGreenOrchestrationWorkflowBuilder.aBlueGreenOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
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
import static software.wings.service.impl.workflow.WorkflowServiceHelper.VERIFY_SERVICE;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.api.client.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class WorkflowUtils {
  private static final long TEST_TIMEOUT_IN_MINUTES = 8;

  static final String SETUP_CONTAINER_CONSTANT = "Setup Container";
  static final String PRE_DEPLOYMENT_CONSTANT = "Pre-Deployment";
  static final String ECS_DAEMON_SERVICE_SETUP_NAME = "ECS Daemon Service Setup";
  static final String POST_DEPLOYMENT_CONSTANT = "Post-Deployment";
  static final String WRAP_UP_CONSTANT = "Wrap Up";
  static final String ECS_SERVICE_SETUP_CONSTANT = "ECS Service Setup";
  static final String UPGRADE_CONTAINERS_CONSTANT = "Upgrade Containers";
  static final String DEPLOY_CONTAINERS_CONSTANT = "Deploy Containers";
  static final String SELECT_NODES_CONSTANT = "Select Node";

  @Inject private WorkflowExecutionService workflowExecutionService;

  public void checkForWorkflowSuccess(WorkflowExecution workflowExecution) {
    Awaitility.await().atMost(TEST_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus status =
          workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid())
              .getStatus();
      return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED;
    });
    WorkflowExecution finalWorkflowExecution =
        workflowExecutionService.getWorkflowExecution(workflowExecution.getAppId(), workflowExecution.getUuid());
    if (finalWorkflowExecution.getStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException(
          "workflow execution did not succeed. Final status: " + finalWorkflowExecution.getStatus());
    }
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
    phaseSteps.add(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, PhaseStepType.DEPLOY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.ENABLE_SERVICE, PhaseStepType.ENABLE_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, PhaseStepType.VERIFY_SERVICE.name()).build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());
    Workflow workflow =
        aWorkflow()
            .name(name)
            .appId(service.getAppId())
            .serviceId(service.getUuid())
            .envId(infrastructureDefinition.getEnvId())
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                    .withWorkflowPhases(Arrays.asList(aWorkflowPhase()
                                                          .serviceId(service.getUuid())
                                                          .infraDefinitionId(infrastructureDefinition.getUuid())
                                                          .phaseSteps(phaseSteps)
                                                          .build()))
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                    .build())
            .build();

    return workflow;
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

    Workflow workflow =
        aWorkflow()
            .name(name + System.currentTimeMillis())
            .appId(service.getAppId())
            .envId(defaultInfrastructureDefinition.getEnvId())
            .infraDefinitionId(defaultInfrastructureDefinition.getUuid())
            .serviceId(service.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aRollingOrchestrationWorkflow().build())
            .templatized(true)
            .templateExpressions(Arrays.asList(templateExpressionForEnv, templateExpressionForInfraDefinition))
            .build();
    return workflow;
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
                                                    .put("minInstances", 1)
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
            .name(name + System.currentTimeMillis())
            .appId(appId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aBuildOrchestrationWorkflow()
                    .withWorkflowPhases(Arrays.asList(aWorkflowPhase().phaseSteps(phaseSteps).build()))
                    .build())
            .build();
    return buildWorkflow;
  }

  private TemplateExpression getTemplateExpressionsForEnv() {
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

  private TemplateExpression getTemplateExpressionsForInfraDefinition(String expression) {
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
}
