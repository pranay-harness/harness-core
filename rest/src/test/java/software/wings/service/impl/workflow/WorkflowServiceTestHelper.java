package software.wings.service.impl.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BlueGreenOrchestrationWorkflow.BlueGreenOrchestrationWorkflowBuilder.aBlueGreenOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.InfrastructureMappingType.AWS_AMI;
import static software.wings.beans.InfrastructureMappingType.AWS_ECS;
import static software.wings.beans.InfrastructureMappingType.GCP_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.PipelineStage.PipelineStageElement.builder;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.DEPLOY_CONTAINERS;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.UPGRADE_CONTAINERS;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.WAIT;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_COMMAND_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TARGET_SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;

import software.wings.api.DeploymentType;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ExecutionScope;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.RepairActionCode;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.stats.CloneMetadata;
import software.wings.common.Constants;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowServiceTestHelper {
  public static Workflow constructCustomWorkflow() {
    Graph graph =
        aGraph()
            .addNodes(aGraphNode().withId("n1").withName("stop").withType(ENV_STATE.name()).withOrigin(true).build(),
                aGraphNode().withId("n2").withName("wait").withType(WAIT.name()).addProperty("duration", 1l).build(),
                aGraphNode().withId("n3").withName("start").withType(ENV_STATE.name()).build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
            .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
            .build();

    CustomOrchestrationWorkflow orchestrationWorkflow = aCustomOrchestrationWorkflow().withGraph(graph).build();
    return aWorkflow()
        .withAppId(APP_ID)
        .withName("workflow1")
        .withDescription("Sample Workflow")
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(orchestrationWorkflow)
        .build();
  }

  public static Workflow constructCanaryWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withEnvId(ENV_ID)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructCanaryWorkflowWithPhase() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructCanaryWorkflowWithTwoPhases() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .addWorkflowPhase(
                    aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build())
                .addWorkflowPhase(
                    aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build())
                .build())
        .build();
  }

  public static Workflow constructCanaryWithHttpStep() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).addStep(constructHttpStep()).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructCanaryWithHttpPhaseStep() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(
                    aWorkflowPhase()
                        .withInfraMappingId(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH)
                        .addPhaseStep(
                            aPhaseStep(VERIFY_SERVICE, Constants.ENABLE_SERVICE).addStep(constructHttpStep()).build())
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructCanaryHttpAsPostDeploymentStep() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPostDeploymentSteps(
                    aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(constructHttpStep()).build())
                .build())
        .build();
  }

  private static GraphNode constructHttpStep() {
    return aGraphNode().withType("HTTP").withName("http").addProperty("url", "http://www.google.com").build();
  }

  public static Workflow constructBasicWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withEnvId(ENV_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructBlueGreenWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withEnvId(ENV_ID)
        .withOrchestrationWorkflow(
            aBlueGreenOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructBasicWorkflowWithPhase() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withEnvId(ENV_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .build())
        .build();
  }

  public static Workflow constructBuildWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withOrchestrationWorkflow(aBuildOrchestrationWorkflow().build())
        .build();
  }

  public static Workflow constructMultiServiceWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withEnvId(ENV_ID)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aMultiServiceOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructMultiServiceWorkflowWithPhase() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withEnvId(ENV_ID)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aMultiServiceOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructMulitServiceTemplateWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aMultiServiceOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructTemplatizedCanaryWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static ServiceCommand constructServiceCommand() {
    return aServiceCommand()
        .withServiceId(SERVICE_ID)
        .withUuid(SERVICE_COMMAND_ID)
        .withAppId(APP_ID)
        .withName("START")
        .withCommand(
            aCommand()
                .withName("START")
                .addCommandUnits(
                    anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build())
                .build())
        .build();
  }

  public static void assertClonedWorkflowAcrossApps(Workflow workflow2, Workflow clonedWorkflow) {
    assertThat(clonedWorkflow).isNotNull();
    assertThat(clonedWorkflow.getUuid()).isNotEqualTo(workflow2.getUuid());
    assertThat(clonedWorkflow.getAppId()).isEqualTo(TARGET_APP_ID);
    assertThat(clonedWorkflow.getEnvId()).isNullOrEmpty();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow clonedOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) clonedWorkflow.getOrchestrationWorkflow();
    assertThat(clonedOrchestrationWorkflow).isNotNull();
    assertThat(clonedOrchestrationWorkflow.getOrchestrationWorkflowType())
        .isEqualTo(orchestrationWorkflow.getOrchestrationWorkflowType());
    assertThat(clonedOrchestrationWorkflow.isValid()).isFalse();
    assertThat(clonedOrchestrationWorkflow.getValidationMessage()).startsWith("Environment");
    List<WorkflowPhase> workflowPhases = clonedOrchestrationWorkflow.getWorkflowPhases();
    assertThat(workflowPhases).extracting(workflowPhase -> workflowPhase.getServiceId()).contains(TARGET_SERVICE_ID);
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getInfraMappingId())
        .containsNull();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getInfraMappingName())
        .containsNull();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getComputeProviderId())
        .containsNull();
  }

  public static void assertOrchestrationWorkflow(CanaryOrchestrationWorkflow orchestrationWorkflow) {
    assertThat(orchestrationWorkflow)
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    assertThat(orchestrationWorkflow.getGraph()).isNotNull();
    assertThat(orchestrationWorkflow.getGraph().getNodes())
        .extracting("id")
        .contains(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    assertThat(orchestrationWorkflow.getGraph().getSubworkflows())
        .containsKeys(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    assertThat(orchestrationWorkflow.getNotificationRules()).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0)).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0).getConditions()).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0).getExecutionScope())
        .isEqualTo(ExecutionScope.WORKFLOW);

    if (!(orchestrationWorkflow instanceof BuildWorkflow)) {
      assertThat(orchestrationWorkflow.getFailureStrategies()).isNotNull();
      assertThat(orchestrationWorkflow.getFailureStrategies().get(0)).isNotNull();
      assertThat(orchestrationWorkflow.getFailureStrategies().get(0).getRepairActionCode())
          .isEqualTo(RepairActionCode.ROLLBACK_WORKFLOW);
    }
  }

  public static Workflow constructWorkflowWithParam(PhaseStep phaseStep) {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .addPhaseStep(phaseStep)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static Workflow constructEcsWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withEnvId(ENV_ID)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withServiceId(SERVICE_ID)
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                                                        .addStep(aGraphNode()
                                                                     .withId(generateUuid())
                                                                     .withType(ECS_SERVICE_DEPLOY.name())
                                                                     .withName(UPGRADE_CONTAINERS)
                                                                     .build())
                                                        .build())
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  public static GcpKubernetesInfrastructureMapping constructGKInfraMapping() {
    return aGcpKubernetesInfrastructureMapping()
        .withUuid(INFRA_MAPPING_ID)
        .withServiceId(SERVICE_ID)
        .withDeploymentType(DeploymentType.KUBERNETES.name())
        .withInfraMappingType(GCP_KUBERNETES.name())
        .withComputeProviderType(GCP.name())
        .withClusterName(Constants.RUNTIME)
        .build();
  }

  public static PhysicalInfrastructureMapping constructPhysicalInfraMapping() {
    return aPhysicalInfrastructureMapping()
        .withUuid(INFRA_MAPPING_ID)
        .withServiceId(SERVICE_ID)
        .withDeploymentType(DeploymentType.SSH.name())
        .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
        .withComputeProviderType(PHYSICAL_DATA_CENTER_SSH.name())
        .build();
  }

  public static DirectKubernetesInfrastructureMapping constructDirectKubernetesInfra() {
    return aDirectKubernetesInfrastructureMapping()
        .withUuid(INFRA_MAPPING_ID)
        .withServiceId(SERVICE_ID)
        .withDeploymentType(DeploymentType.KUBERNETES.name())
        .withComputeProviderType(AWS.name())
        .build();
  }

  public static EcsInfrastructureMapping constructEcsnfraMapping() {
    return anEcsInfrastructureMapping()
        .withUuid(INFRA_MAPPING_ID)
        .withServiceId(SERVICE_ID)
        .withDeploymentType(DeploymentType.ECS.name())
        .withInfraMappingType(AWS_ECS.name())
        .withComputeProviderType(GCP.name())
        .withClusterName(Constants.RUNTIME)
        .build();
  }

  public static EcsInfrastructureMapping constructAmiInfraMapping() {
    return anEcsInfrastructureMapping()
        .withUuid(INFRA_MAPPING_ID)
        .withServiceId(SERVICE_ID)
        .withDeploymentType(DeploymentType.AMI.name())
        .withInfraMappingType(AWS_AMI.name())
        .withComputeProviderType(AWS.name())
        .build();
  }

  public static AwsLambdaInfraStructureMapping constructAwsLambdaInfraMapping() {
    AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
    awsLambdaInfraStructureMapping.setUuid(INFRA_MAPPING_ID);
    awsLambdaInfraStructureMapping.setServiceId(SERVICE_ID);
    awsLambdaInfraStructureMapping.setDeploymentType(DeploymentType.AWS_LAMBDA.name());
    awsLambdaInfraStructureMapping.setComputeProviderType(AWS.name());

    return awsLambdaInfraStructureMapping;
  }

  public static CloneMetadata constructCloneMetadata(Workflow workflow2) {
    return CloneMetadata.builder()
        .workflow(workflow2)
        .serviceMapping(ImmutableMap.of(SERVICE_ID, TARGET_SERVICE_ID))
        .targetAppId(TARGET_APP_ID)
        .build();
  }

  public static Pipeline constructPipeline(String workflowId) {
    return Pipeline.builder()
        .name("PIPELINE_NAME")
        .pipelineStages(
            asList(PipelineStage.builder()
                       .pipelineStageElements(asList(builder()
                                                         .name("STAGE")
                                                         .type(ENV_STATE.name())
                                                         .properties(ImmutableMap.of("workflowId", workflowId))
                                                         .build()))
                       .build()))
        .build();
  }

  public static Workflow constructBasicDeploymentTemplateWorkflow() {
    return aWorkflow()
        .withEnvId(ENV_ID)
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withWorkflowPhases(asList(aWorkflowPhase()
                                               .withServiceId(SERVICE_ID)
                                               .withInfraMappingId(INFRA_MAPPING_ID)
                                               .withTemplateExpressions(asList(getEnvTemplateExpression(),
                                                   getServiceTemplateExpression(), getInfraTemplateExpression()))
                                               .build()))
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .withTemplateExpressions(
            asList(getEnvTemplateExpression(), getServiceTemplateExpression(), getInfraTemplateExpression()))
        .build();
  }

  public static Workflow constructLinkedTemplate(GraphNode step) {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withUuid(WORKFLOW_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).addStep(step).build())
                .addWorkflowPhase(
                    aWorkflowPhase()
                        .withInfraMappingId(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH)
                        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE).addStep(step).build())
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(step).build())
                .build())
        .build();
  }

  public static void assertTemplatizedWorkflow(Workflow workflow3) {
    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);
  }

  public static List<TemplateExpression> constructAppdTemplateExpressions() {
    return asList(TemplateExpression.builder()
                      .fieldName("analysisServerConfigId")
                      .expression("${AppDynamics_Server}")
                      .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                      .build(),
        TemplateExpression.builder()
            .fieldName("applicationId")
            .expression("${AppDynamics_App}")
            .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_APPID"))
            .build(),
        TemplateExpression.builder()
            .fieldName("tierId")
            .expression("${AppDynamics_Tier}")
            .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_TIERID"))
            .build());
  }

  public static List<TemplateExpression> constructElkTemplateExpressions() {
    return asList(TemplateExpression.builder()
                      .fieldName("analysisServerConfigId")
                      .expression("${ELK_Server}")
                      .metadata(ImmutableMap.of("entityType", "ELK_CONFIGID"))
                      .build(),
        TemplateExpression.builder()
            .fieldName("indices")
            .expression("${ELK_Indices}")
            .metadata(ImmutableMap.of("entityType", "ELK_INDICES"))
            .build());
  }

  public static PhaseStep constructAppDVerifyStep(WorkflowPhase workflowPhase) {
    PhaseStep verifyPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE)
                                    .collect(toList())
                                    .get(0);

    verifyPhaseStep.getSteps().add(aGraphNode()
                                       .withType("APP_DYNAMICS")
                                       .withName("APP_DYNAMICS")
                                       .addProperty("analysisServerConfigId", "analysisServerConfigId")
                                       .addProperty("applicationId", "applicagionId")
                                       .addProperty("tierId", "tierId")
                                       .build());

    verifyPhaseStep.getSteps().add(aGraphNode()
                                       .withType("ELK")
                                       .withName("ELK")
                                       .addProperty("analysisServerConfigId", "analysisServerConfigId")
                                       .addProperty("indices", "indices")
                                       .build());
    return verifyPhaseStep;
  }

  public static TemplateExpression getEnvTemplateExpression() {
    return TemplateExpression.builder()
        .fieldName("envId")
        .expression("${Environment}")
        .metadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
        .build();
  }

  public static TemplateExpression getServiceTemplateExpression() {
    return TemplateExpression.builder()
        .fieldName("serviceId")
        .expression("${Service}")
        .metadata(ImmutableMap.of("entityType", "SERVICE"))
        .build();
  }

  public static TemplateExpression getInfraTemplateExpression() {
    return TemplateExpression.builder()
        .fieldName("infraMappingId")
        .expression("${ServiceInfra_SSH}")
        .metadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
        .build();
  }

  public static GraphNode constructHttpTemplateStep() {
    return aGraphNode()
        .withType(StateType.HTTP.name())
        .withTemplateUuid(TEMPLATE_ID)
        .withProperties(constructHttpProperties())
        .withTemplateVariables(
            asList(aVariable().withName("url").withValue("https://harness.io").build(), aVariable().build()))
        .build();
  }

  private static Map<String, Object> constructHttpProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("url", "${url}");
    properties.put("method", "GET");
    properties.put("assertion", "${assertion}");
    return properties;
  }

  public static void assertLinkedPhaseStep(
      PhaseStep phaseStep, GraphNode preDeploymentStep, PhaseStep updatedPhaseStep) {
    assertThat(updatedPhaseStep).isNotNull();
    GraphNode updatedPreStep = phaseStep.getSteps().stream().findFirst().orElse(null);
    assertLinkedStep(updatedPreStep);
    assertThat(preDeploymentStep.getProperties()).isNotEmpty().containsKeys("url", "method");
    assertThat(preDeploymentStep.getProperties()).isNotEmpty().doesNotContainValue("200 OK");
  }

  public static void assertLinkedStep(GraphNode updatedPreStep) {
    assertThat(updatedPreStep).isNotNull();
    assertThat(updatedPreStep.getTemplateUuid()).isNotEmpty().isEqualTo(TEMPLATE_ID);
    assertThat(updatedPreStep.getTemplateVersion()).isNotEmpty().isEqualTo("1");

    assertThat(updatedPreStep.getTemplateVariables()).isNotEmpty();

    assertThat(updatedPreStep.getTemplateVariables()).isNotEmpty().extracting(Variable::getName).contains("url");
    assertThat(updatedPreStep.getTemplateVariables())
        .isNotEmpty()
        .extracting(Variable::getValue)
        .contains("https://harness.io");
    assertThat(updatedPreStep.getTemplateVariables())
        .isNotEmpty()
        .extracting(Variable::getValue)
        .doesNotContain("200 OK");
  }

  public static void assertTemplatizedOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().doesNotContain(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH2")))
        .isTrue();
  }

  public static void assertTemplateWorkflowPhase(
      OrchestrationWorkflow orchestrationWorkflow, List<WorkflowPhase> workflowPhases) {
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNullOrEmpty();
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);
  }

  public static WorkflowPhase assertWorkflowPhaseTemplateExpressions(Workflow workflow, WorkflowPhase workflowPhase) {
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);
    return workflowPhase3;
  }

  public static void assertTemplateStep(GraphNode preDeploymentStep) {
    assertThat(preDeploymentStep).isNotNull();
    assertThat(preDeploymentStep.getTemplateUuid()).isNotEmpty().isEqualTo(TEMPLATE_ID);
    assertThat(preDeploymentStep.getTemplateVersion()).isNotEmpty().isEqualTo(LATEST_TAG);

    List<Variable> templateVariables = preDeploymentStep.getTemplateVariables();
    assertThat(templateVariables).isNotEmpty();

    preDeploymentStep.setTemplateVersion("1");
    preDeploymentStep.setTemplateVariables(asList(aVariable().withName("url").withValue("https://google.com").build()));
  }

  public static void assertPhaseNode(GraphNode updatedPhaseNode) {
    assertThat(updatedPhaseNode).isNotNull();
    assertThat(updatedPhaseNode.getTemplateUuid()).isNotEmpty().isEqualTo(TEMPLATE_ID);
    assertThat(updatedPhaseNode.getTemplateVersion()).isNotEmpty().isEqualTo("1");

    assertThat(updatedPhaseNode.getTemplateVariables()).isNotEmpty();

    assertThat(updatedPhaseNode.getTemplateVariables()).isNotEmpty().extracting(Variable::getName).contains("url");
    assertThat(updatedPhaseNode.getTemplateVariables())
        .isNotEmpty()
        .extracting(Variable::getValue)
        .contains("https://harness.io");
    assertThat(updatedPhaseNode.getTemplateVariables())
        .isNotEmpty()
        .extracting(Variable::getValue)
        .doesNotContain("200 OK");
  }

  public static void assertWorkflowPhaseTemplateStep(
      GraphNode preDeploymentStep, GraphNode postDeploymentStep, GraphNode phaseNode) {
    assertThat(phaseNode).isNotNull();
    assertThat(phaseNode.getTemplateUuid()).isNotEmpty().isEqualTo(TEMPLATE_ID);
    assertThat(postDeploymentStep.getTemplateVersion()).isNotEmpty().isEqualTo(LATEST_TAG);
    assertThat(preDeploymentStep.getTemplateVariables()).isNotEmpty().extracting(Variable::getName).contains("url");
    assertThat(preDeploymentStep.getTemplateVariables())
        .isNotEmpty()
        .extracting(Variable::getValue)
        .contains("https://harness.io");
    assertThat(preDeploymentStep.getProperties()).isNotEmpty().containsKeys("url", "method", "assertion");
  }

  public static void assertPostDeployTemplateStep(GraphNode preDeploymentStep, GraphNode postDeploymentStep) {
    assertThat(postDeploymentStep).isNotNull();
    assertThat(postDeploymentStep.getTemplateUuid()).isNotEmpty().isEqualTo(TEMPLATE_ID);
    assertThat(postDeploymentStep.getTemplateVersion()).isNotEmpty().isEqualTo(LATEST_TAG);
    assertThat(preDeploymentStep.getTemplateVariables()).isNotEmpty().extracting(Variable::getName).contains("url");
    assertThat(preDeploymentStep.getProperties()).isNotEmpty().containsKeys("url", "method", "assertion");
  }

  public static void assertPreDeployTemplateStep(GraphNode preDeploymentStep) {
    assertWorkflowPhaseTemplateStep(preDeploymentStep, preDeploymentStep, preDeploymentStep);
  }
}
