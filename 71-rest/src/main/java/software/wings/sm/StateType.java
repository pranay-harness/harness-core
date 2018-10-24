package software.wings.sm;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.joor.Reflect.on;
import static software.wings.beans.PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_AWS_LAMBDA;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.ROUTE_UPDATE;
import static software.wings.beans.PhaseStepType.SELECT_NODE;
import static software.wings.beans.PhaseStepType.START_SERVICE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.common.Constants.AMI_SETUP_COMMAND_NAME;
import static software.wings.common.Constants.AWS_CODE_DEPLOY;
import static software.wings.common.Constants.AWS_LAMBDA;
import static software.wings.common.Constants.DE_PROVISION_CLOUD_FORMATION;
import static software.wings.common.Constants.K8S_DEPLOYMENT_ROLLING_ROLLBAK;
import static software.wings.common.Constants.KUBERNETES_SERVICE_SETUP;
import static software.wings.common.Constants.PCF_UNMAP_ROUT;
import static software.wings.common.Constants.PROVISION_CLOUD_FORMATION;
import static software.wings.common.Constants.ROLLBACK_AWS_AMI_CLUSTER;
import static software.wings.common.Constants.ROLLBACK_AWS_CODE_DEPLOY;
import static software.wings.common.Constants.ROLLBACK_AWS_LAMBDA;
import static software.wings.common.Constants.ROLLBACK_CLOUD_FORMATION;
import static software.wings.common.Constants.ROLLBACK_CONTAINERS;
import static software.wings.common.Constants.ROLLBACK_KUBERNETES_SETUP;
import static software.wings.common.Constants.ROLLBACK_TERRAFORM_NAME;
import static software.wings.common.Constants.SELECT_NODE_NAME;
import static software.wings.common.Constants.UPGRADE_AUTOSCALING_GROUP;
import static software.wings.common.Constants.UPGRADE_CONTAINERS;
import static software.wings.sm.StateTypeScope.COMMON;
import static software.wings.sm.StateTypeScope.NONE;
import static software.wings.sm.StateTypeScope.ORCHESTRATION_STENCILS;
import static software.wings.sm.StateTypeScope.PIPELINE_STENCILS;
import static software.wings.stencils.StencilCategory.CLOUD;
import static software.wings.stencils.StencilCategory.COLLECTIONS;
import static software.wings.stencils.StencilCategory.COMMANDS;
import static software.wings.stencils.StencilCategory.CONTROLS;
import static software.wings.stencils.StencilCategory.ENVIRONMENTS;
import static software.wings.stencils.StencilCategory.FLOW_CONTROLS;
import static software.wings.stencils.StencilCategory.KUBERNETES;
import static software.wings.stencils.StencilCategory.OTHERS;
import static software.wings.stencils.StencilCategory.PROVISIONERS;
import static software.wings.stencils.StencilCategory.VERIFICATIONS;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.sm.states.APMVerificationState;
import software.wings.sm.states.AppDynamicsState;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.ArtifactCheckState;
import software.wings.sm.states.ArtifactCollectionState;
import software.wings.sm.states.AwsAmiServiceDeployState;
import software.wings.sm.states.AwsAmiServiceRollback;
import software.wings.sm.states.AwsAmiServiceSetup;
import software.wings.sm.states.AwsCodeDeployRollback;
import software.wings.sm.states.AwsCodeDeployState;
import software.wings.sm.states.AwsLambdaRollback;
import software.wings.sm.states.AwsLambdaState;
import software.wings.sm.states.AwsNodeSelectState;
import software.wings.sm.states.AzureNodeSelectState;
import software.wings.sm.states.BambooState;
import software.wings.sm.states.BarrierState;
import software.wings.sm.states.BugsnagState;
import software.wings.sm.states.CloudWatchState;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.CustomLogVerificationState;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.DcNodeSelectState;
import software.wings.sm.states.DynatraceState;
import software.wings.sm.states.EcsServiceDeploy;
import software.wings.sm.states.EcsServiceRollback;
import software.wings.sm.states.EcsServiceSetup;
import software.wings.sm.states.EcsSteadyStateCheck;
import software.wings.sm.states.ElasticLoadBalancerState;
import software.wings.sm.states.ElkAnalysisState;
import software.wings.sm.states.EmailState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.GcpClusterSetup;
import software.wings.sm.states.HelmDeployState;
import software.wings.sm.states.HelmRollbackState;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.KubernetesDeploy;
import software.wings.sm.states.KubernetesDeployRollback;
import software.wings.sm.states.KubernetesSetup;
import software.wings.sm.states.KubernetesSetupRollback;
import software.wings.sm.states.KubernetesSteadyStateCheck;
import software.wings.sm.states.KubernetesSwapServiceSelectors;
import software.wings.sm.states.LogzAnalysisState;
import software.wings.sm.states.NewRelicDeploymentMarkerState;
import software.wings.sm.states.NewRelicState;
import software.wings.sm.states.PauseState;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;
import software.wings.sm.states.PrometheusState;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.ResourceConstraintState;
import software.wings.sm.states.RollingNodeSelectState;
import software.wings.sm.states.ShellScriptState;
import software.wings.sm.states.SplunkState;
import software.wings.sm.states.SplunkV2State;
import software.wings.sm.states.SubWorkflowState;
import software.wings.sm.states.SumoLogicAnalysisState;
import software.wings.sm.states.WaitState;
import software.wings.sm.states.k8s.K8sDeploymentRollingSetup;
import software.wings.sm.states.pcf.MapRouteState;
import software.wings.sm.states.pcf.PcfDeployState;
import software.wings.sm.states.pcf.PcfRollbackState;
import software.wings.sm.states.pcf.PcfSetupState;
import software.wings.sm.states.pcf.PcfSwitchBlueGreenRoutes;
import software.wings.sm.states.pcf.UnmapRouteState;
import software.wings.sm.states.provision.ApplyTerraformProvisionState;
import software.wings.sm.states.provision.CloudFormationCreateStackState;
import software.wings.sm.states.provision.CloudFormationDeleteStackState;
import software.wings.sm.states.provision.CloudFormationRollbackStackState;
import software.wings.sm.states.provision.DestroyTerraformProvisionState;
import software.wings.sm.states.provision.TerraformRollbackState;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents type of state.
 *
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateType implements StateTypeDescriptor {
  /**
   * Subworkflow state type.
   */
  SUB_WORKFLOW(SubWorkflowState.class, CONTROLS, 0, asList(), ORCHESTRATION_STENCILS),

  /**
   * Repeat state type.
   */
  REPEAT(RepeatState.class, CONTROLS, 1, asList(), ORCHESTRATION_STENCILS),

  /**
   * Fork state type.
   */
  FORK(ForkState.class, CONTROLS, 2, asList(), ORCHESTRATION_STENCILS),

  /**
   * Wait state type.
   */
  WAIT(WaitState.class, CONTROLS, 3, asList(), ORCHESTRATION_STENCILS),

  /**
   * Pause state type.
   */
  PAUSE(PauseState.class, CONTROLS, 4, "Manual Step", asList(), ORCHESTRATION_STENCILS),

  /**
   * Barrier state type.
   */
  BARRIER(BarrierState.class, FLOW_CONTROLS, 0, "Barrier", asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Resource Constraint state type.
   */
  RESOURCE_CONSTRAINT(
      ResourceConstraintState.class, FLOW_CONTROLS, 0, "Resource Constraint", asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Script state type.
   */
  SHELL_SCRIPT(ShellScriptState.class, OTHERS, 1, "Shell Script", asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Http state type.
   */
  HTTP(HttpState.class, OTHERS, 2, "HTTP", asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Email state type.
   */
  EMAIL(EmailState.class, OTHERS, 3, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * App dynamics state type.
   */
  APP_DYNAMICS(AppDynamicsState.class, VERIFICATIONS, 2, asList(), ORCHESTRATION_STENCILS),

  /**
   * New relic state type.
   */
  NEW_RELIC(NewRelicState.class, VERIFICATIONS, 3, asList(), ORCHESTRATION_STENCILS),

  /**
   * dyna trace state type.
   */
  DYNA_TRACE(DynatraceState.class, VERIFICATIONS, 4, asList(), ORCHESTRATION_STENCILS),

  /**
   * Prometheus state type.
   */
  PROMETHEUS(PrometheusState.class, VERIFICATIONS, 5, asList(), ORCHESTRATION_STENCILS),

  /**
   * Splunk state type.
   */
  SPLUNK(SplunkState.class, VERIFICATIONS, 6, asList(), ORCHESTRATION_STENCILS),

  /**
   * Splunk V2 state type.
   */
  SPLUNKV2(SplunkV2State.class, VERIFICATIONS, 7, asList(), ORCHESTRATION_STENCILS),

  /**
   * Elk state type.
   */
  ELK(ElkAnalysisState.class, VERIFICATIONS, 8, "ELK", emptyList(), ORCHESTRATION_STENCILS),

  /**
   * LOGZ state type.
   */
  LOGZ(LogzAnalysisState.class, VERIFICATIONS, 9, "LOGZ", emptyList(), ORCHESTRATION_STENCILS),

  /**
   * Sumo state type.
   */
  SUMO(SumoLogicAnalysisState.class, VERIFICATIONS, 10, "SumoLogic", emptyList(), ORCHESTRATION_STENCILS),

  /**
   * Sumo state type.
   */
  DATA_DOG(DatadogState.class, VERIFICATIONS, 11, "Datadog", emptyList(), ORCHESTRATION_STENCILS),

  /**
   * Cloud watch state type.
   */
  CLOUD_WATCH(CloudWatchState.class, VERIFICATIONS, 12, "CloudWatch", asList(), ORCHESTRATION_STENCILS),

  /**
   * New relic deployment marker state type.
   */
  NEW_RELIC_DEPLOYMENT_MARKER(NewRelicDeploymentMarkerState.class, VERIFICATIONS, 13, "NewRelic Deployment Marker",
      asList(), ORCHESTRATION_STENCILS),

  AWS_LAMBDA_VERIFICATION(
      AwsLambdaVerification.class, VERIFICATIONS, 14, "AWS Lambda Verification", asList(), ORCHESTRATION_STENCILS),

  /**
   * Generic APM verification state type.
   */
  APM_VERIFICATION(APMVerificationState.class, VERIFICATIONS, 15, "APM Verification", asList(), ORCHESTRATION_STENCILS),

  /**

   * Generic LOG verification state type.
   */
  LOG_VERIFICATION(CustomLogVerificationState.class, VERIFICATIONS, -1, "Log Verification", asList(), NONE),

  /**
   * Bugsnag verification state type.
   */
  BUG_SNAG(BugsnagState.class, VERIFICATIONS, 17, "Bugsnag", asList(), ORCHESTRATION_STENCILS),

  /**
   * Env state state type.
   */
  ENV_STATE(EnvState.class, ENVIRONMENTS, asList(), PIPELINE_STENCILS),

  /**
   * Command state type.
   */
  COMMAND(CommandState.class, COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH, InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH,
          InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM, InfrastructureMappingType.AZURE_INFRA),
      asList(START_SERVICE, STOP_SERVICE, DEPLOY_SERVICE, ENABLE_SERVICE, DISABLE_SERVICE), ORCHESTRATION_STENCILS),

  /**
   * Approval state type for pipeline and workflow.
   */
  APPROVAL(ApprovalState.class, OTHERS, asList(), PIPELINE_STENCILS, ORCHESTRATION_STENCILS, COMMON),

  /**
   * The Load balancer.
   */
  ELASTIC_LOAD_BALANCER(ElasticLoadBalancerState.class, COMMANDS, "Elastic Load Balancer",
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH, InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH),
      asList(ENABLE_SERVICE, DISABLE_SERVICE), ORCHESTRATION_STENCILS),

  /**
   * Jenkins state type.
   */
  JENKINS(JenkinsState.class, OTHERS, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Bamboo state type
   */
  BAMBOO(BambooState.class, OTHERS, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Artifact Collection state type.
   */
  ARTIFACT_COLLECTION(ArtifactCollectionState.class, COLLECTIONS, Constants.ARTIFACT_COLLECTION, emptyList(),
      emptyList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Artifact Collection state type.
   */
  ARTIFACT_CHECK(ArtifactCheckState.class, OTHERS, 4, "Artifact Check", asList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  AZURE_NODE_SELECT(AzureNodeSelectState.class, CLOUD, SELECT_NODE_NAME,
      Lists.newArrayList(InfrastructureMappingType.AZURE_INFRA), asList(INFRASTRUCTURE_NODE, SELECT_NODE),
      ORCHESTRATION_STENCILS),

  /**
   * AWS Node Select state.
   */
  AWS_NODE_SELECT(AwsNodeSelectState.class, CLOUD, SELECT_NODE_NAME,
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH), asList(INFRASTRUCTURE_NODE, SELECT_NODE),
      ORCHESTRATION_STENCILS),

  DC_NODE_SELECT(DcNodeSelectState.class, CLOUD, SELECT_NODE_NAME,
      Lists.newArrayList(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH), asList(SELECT_NODE),
      ORCHESTRATION_STENCILS),

  ROLLING_NODE_SELECT(
      RollingNodeSelectState.class, CLOUD, SELECT_NODE_NAME, asList(), asList(SELECT_NODE), ORCHESTRATION_STENCILS),

  PHASE(PhaseSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  PHASE_STEP(PhaseStepSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  AWS_CODEDEPLOY_STATE(AwsCodeDeployState.class, COMMANDS, AWS_CODE_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_CODEDEPLOY), asList(DEPLOY_AWSCODEDEPLOY),
      ORCHESTRATION_STENCILS),

  AWS_CODEDEPLOY_ROLLBACK(AwsCodeDeployRollback.class, COMMANDS, ROLLBACK_AWS_CODE_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_CODEDEPLOY), asList(DEPLOY_AWSCODEDEPLOY),
      ORCHESTRATION_STENCILS),

  AWS_LAMBDA_STATE(AwsLambdaState.class, COMMANDS, AWS_LAMBDA,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_LAMBDA), asList(DEPLOY_AWS_LAMBDA), ORCHESTRATION_STENCILS),

  AWS_LAMBDA_ROLLBACK(AwsLambdaRollback.class, COMMANDS, ROLLBACK_AWS_LAMBDA,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_LAMBDA), asList(DEPLOY_AWS_LAMBDA), ORCHESTRATION_STENCILS),

  AWS_AMI_SERVICE_SETUP(AwsAmiServiceSetup.class, CLOUD, AMI_SETUP_COMMAND_NAME,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  AWS_AMI_SERVICE_DEPLOY(AwsAmiServiceDeployState.class, COMMANDS, UPGRADE_AUTOSCALING_GROUP,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(AMI_DEPLOY_AUTOSCALING_GROUP),
      ORCHESTRATION_STENCILS),

  AWS_AMI_SERVICE_ROLLBACK(AwsAmiServiceRollback.class, COMMANDS, ROLLBACK_AWS_AMI_CLUSTER,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(AMI_DEPLOY_AUTOSCALING_GROUP),
      ORCHESTRATION_STENCILS),

  ECS_SERVICE_SETUP(EcsServiceSetup.class, CLOUD, Constants.ECS_SERVICE_SETUP,
      Lists.newArrayList(InfrastructureMappingType.AWS_ECS), asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  ECS_SERVICE_DEPLOY(EcsServiceDeploy.class, COMMANDS, UPGRADE_CONTAINERS,
      Lists.newArrayList(InfrastructureMappingType.AWS_ECS), asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_SERVICE_ROLLBACK(EcsServiceRollback.class, COMMANDS, ROLLBACK_CONTAINERS,
      Lists.newArrayList(InfrastructureMappingType.AWS_ECS), asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  KUBERNETES_SETUP(KubernetesSetup.class, CLOUD, KUBERNETES_SERVICE_SETUP,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  KUBERNETES_SETUP_ROLLBACK(KubernetesSetupRollback.class, COMMANDS, ROLLBACK_KUBERNETES_SETUP,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  KUBERNETES_DEPLOY(KubernetesDeploy.class, COMMANDS, UPGRADE_CONTAINERS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_DEPLOY, WRAP_UP), ORCHESTRATION_STENCILS),

  KUBERNETES_DEPLOY_ROLLBACK(KubernetesDeployRollback.class, COMMANDS, ROLLBACK_CONTAINERS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  KUBERNETES_STEADY_STATE_CHECK(KubernetesSteadyStateCheck.class, COMMANDS, Constants.KUBERNETES_STEADY_STATE_CHECK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_STEADY_STATE_CHECK(EcsSteadyStateCheck.class, COMMANDS, Constants.ECS_STEADY_STATE_CHECK,
      Lists.newArrayList(InfrastructureMappingType.AWS_ECS), asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  GCP_CLUSTER_SETUP(GcpClusterSetup.class, CLOUD,
      Lists.newArrayList(InfrastructureMappingType.GCP_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CLUSTER_SETUP), ORCHESTRATION_STENCILS),

  HELM_DEPLOY(HelmDeployState.class, COMMANDS, Constants.HELM_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(PhaseStepType.HELM_DEPLOY), ORCHESTRATION_STENCILS),
  HELM_ROLLBACK(HelmRollbackState.class, COMMANDS, Constants.HELM_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(PhaseStepType.HELM_DEPLOY), ORCHESTRATION_STENCILS),

  PCF_SETUP(PcfSetupState.class, COMMANDS, Constants.PCF_SETUP, Lists.newArrayList(InfrastructureMappingType.PCF_PCF),
      asList(PhaseStepType.PCF_SETUP), ORCHESTRATION_STENCILS),

  PCF_RESIZE(PcfDeployState.class, COMMANDS, Constants.PCF_RESIZE,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  PCF_ROLLBACK(PcfRollbackState.class, COMMANDS, Constants.PCF_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  PCF_MAP_ROUTE(MapRouteState.class, FLOW_CONTROLS, Constants.PCF_MAP_ROUTE,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),
  PCF_UNMAP_ROUTE(UnmapRouteState.class, FLOW_CONTROLS, PCF_UNMAP_ROUT,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),
  PCF_BG_MAP_ROUTE(PcfSwitchBlueGreenRoutes.class, FLOW_CONTROLS, Constants.PCF_BG_MAP_ROUTE,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  TERRAFORM_PROVISION(ApplyTerraformProvisionState.class, PROVISIONERS, 0, "Terraform Provision",
      asList(InfrastructureMappingType.AWS_SSH), asList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  //  TERRAFORM_ADJUST(AdjustTerraformProvisionState.class, PROVISIONERS, asList(InfrastructureMappingType.AWS_SSH),
  //      asList(INFRASTRUCTURE_NODE), ORCHESTRATION_STENCILS),

  TERRAFORM_DESTROY(DestroyTerraformProvisionState.class, PROVISIONERS, 0, "Terraform Destroy",
      asList(InfrastructureMappingType.AWS_SSH), asList(POST_DEPLOYMENT), ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_CREATE_STACK(CloudFormationCreateStackState.class, PROVISIONERS, PROVISION_CLOUD_FORMATION,
      asList(InfrastructureMappingType.AWS_SSH), asList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_DELETE_STACK(CloudFormationDeleteStackState.class, PROVISIONERS, DE_PROVISION_CLOUD_FORMATION,
      asList(InfrastructureMappingType.AWS_SSH), asList(POST_DEPLOYMENT), ORCHESTRATION_STENCILS),

  KUBERNETES_SWAP_SERVICE_SELECTORS(KubernetesSwapServiceSelectors.class, COMMANDS,
      Constants.KUBERNETES_SWAP_SERVICE_SELECTORS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY, ROUTE_UPDATE, WRAP_UP), ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_ROLLBACK_STACK(CloudFormationRollbackStackState.class, PROVISIONERS, ROLLBACK_CLOUD_FORMATION,
      singletonList(InfrastructureMappingType.AWS_SSH), singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  TERRAFORM_ROLLBACK(TerraformRollbackState.class, PROVISIONERS, ROLLBACK_TERRAFORM_NAME,
      singletonList(InfrastructureMappingType.AWS_SSH), singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  K8S_DEPLOYMENT_ROLLING(K8sDeploymentRollingSetup.class, KUBERNETES, Constants.K8S_DEPLOYMENT_ROLLING,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_DEPLOYMENT_ROLLING_ROLLBACK(K8sDeploymentRollingSetup.class, KUBERNETES, K8S_DEPLOYMENT_ROLLING_ROLLBAK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS);

  private static final String stencilsPath = "/templates/stencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private static final Logger logger = LoggerFactory.getLogger(StateType.class);
  private final Class<? extends State> stateClass;
  private final Object jsonSchema;
  private Object uiSchema;
  private List<StateTypeScope> scopes = new ArrayList<>();
  private List<String> phaseStepTypes = new ArrayList<>();
  private final StencilCategory stencilCategory;
  private Integer displayOrder = DEFAULT_DISPLAY_ORDER;
  private String displayName = UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
  private List<InfrastructureMappingType> supportedInfrastructureMappingTypes = emptyList();

  /**
   * Instantiates a new state type.
   *
   * @param stateClass the state class
   * @param scopes     the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, phaseStepTypes, scopes);
  }

  <E> StateType(Class<? extends State> stateClass, StencilCategory stencilCategory,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, supportedInfrastructureMappingTypes, phaseStepTypes,
        scopes);
  }

  /**
   * Instantiates a new state type.
   *
   * @param stateClass   the state class
   * @param displayOrder display order
   * @param scopes       the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, emptyList(), phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, null, supportedInfrastructureMappingTypes, phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, String displayName,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, displayName, supportedInfrastructureMappingTypes,
        phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      String displayName, List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, displayName, emptyList(), phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      String displayName, List<InfrastructureMappingType> supportedInfrastructureMappingTypes,
      List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this.stateClass = stateClass;
    this.scopes = asList(scopes);
    this.phaseStepTypes = phaseStepTypes.stream().map(phaseStepType -> phaseStepType.name()).collect(toList());
    jsonSchema = loadJsonSchema();
    this.stencilCategory = stencilCategory;
    this.displayOrder = displayOrder;
    if (isNotBlank(displayName)) {
      this.displayName = displayName;
    }
    try {
      uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception e) {
      uiSchema = new HashMap<String, Object>();
    }
    this.supportedInfrastructureMappingTypes = supportedInfrastructureMappingTypes;
  }

  private Object readResource(String file) {
    try {
      URL url = getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing StateType-" + file, exception);
    }
  }

  @Override
  @JsonValue
  public String getType() {
    return name();
  }

  @Override
  public Class<? extends State> getTypeClass() {
    return stateClass;
  }

  @Override
  public JsonNode getJsonSchema() {
    return ((JsonNode) jsonSchema).deepCopy();
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public List<StateTypeScope> getScopes() {
    return scopes;
  }

  @Override
  public String getName() {
    return displayName;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.StateTypeDescriptor#newInstance(java.lang.String)
   */
  @Override
  public State newInstance(String id) {
    return on(stateClass).create(id).get();
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingStateTypeDescriptor(this);
  }

  private JsonNode loadJsonSchema() {
    return JsonUtils.jsonSchema(stateClass);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return stencilCategory;
  }

  @Override
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public boolean matches(Object context) {
    if (context instanceof InfrastructureMapping) {
      InfrastructureMapping infrastructureMapping = (InfrastructureMapping) context;
      InfrastructureMappingType infrastructureMappingType =
          InfrastructureMappingType.valueOf(infrastructureMapping.getInfraMappingType());
      return (stencilCategory != COMMANDS && stencilCategory != CLOUD)
          || supportedInfrastructureMappingTypes.contains(infrastructureMappingType);
    }
    DeploymentType deploymentType = (DeploymentType) context;
    List<DeploymentType> supportedDeploymentTypes = new ArrayList<>();
    // TODO: SSH deployment referenced by more than one SelectNode states
    for (InfrastructureMappingType supportedInfrastructureMappingType : supportedInfrastructureMappingTypes) {
      supportedDeploymentTypes.addAll(supportedInfrastructureMappingType.getDeploymentTypes());
    }
    return (stencilCategory != COMMANDS && stencilCategory != CLOUD)
        || supportedDeploymentTypes.contains(deploymentType);
  }

  @Override
  public List<String> getPhaseStepTypes() {
    return phaseStepTypes;
  }

  public boolean isVerificationState() {
    return this.stencilCategory == StencilCategory.VERIFICATIONS;
  }
}
