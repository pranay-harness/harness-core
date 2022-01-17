/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.InfrastructureMappingType.AWS_ECS;
import static software.wings.beans.PhaseStepType.*;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ROUTE_UPDATE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.common.ProvisionerConstants.*;
import static software.wings.common.ProvisionerConstants.PROVISION_SHELL_SCRIPT;
import static software.wings.common.ProvisionerConstants.ROLLBACK_CLOUD_FORMATION;
import static software.wings.common.ProvisionerConstants.ROLLBACK_TERRAFORM_NAME;
import static software.wings.service.impl.aws.model.AwsConstants.AMI_SETUP_COMMAND_NAME;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.*;
import static software.wings.sm.StateTypeScope.*;
import static software.wings.sm.states.k8s.K8sApplyState.K8S_APPLY_STATE;
import static software.wings.sm.states.k8s.K8sTrafficSplitState.K8S_TRAFFIC_SPLIT_STATE_NAME;
import static software.wings.stencils.StencilCategory.*;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.joor.Reflect.on;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.UnexpectedException;
import io.harness.serializer.JsonUtils;

import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.common.ProvisionerConstants;
import software.wings.common.WorkflowConstants;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.aws.model.AwsConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.sm.states.*;
import software.wings.sm.states.azure.*;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotRollback;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotSetup;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotShiftTraffic;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotSwap;
import software.wings.sm.states.collaboration.JiraCreateUpdate;
import software.wings.sm.states.collaboration.ServiceNowCreateUpdateState;
import software.wings.sm.states.customdeployment.InstanceFetchState;
import software.wings.sm.states.k8s.*;
import software.wings.sm.states.pcf.*;
import software.wings.sm.states.provision.*;
import software.wings.sm.states.rancher.RancherK8sCanaryDeploy;
import software.wings.sm.states.rancher.RancherK8sRollingDeploy;
import software.wings.sm.states.rancher.RancherK8sRollingDeployRollback;
import software.wings.sm.states.rancher.RancherResolveState;
import software.wings.sm.states.spotinst.*;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents type of state.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@TargetModule(_870_CG_ORCHESTRATION)
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

  TEMPLATIZED_SECRET_MANAGER(TemplatizedSecretManagerState.class, OTHERS, 3, "Templatized Secret Manager", asList(),
      ORCHESTRATION_STENCILS, COMMON),

  /**
   * Email state type.
   */
  EMAIL(EmailState.class, COLLABORATION, 2,
      asList(PRE_DEPLOYMENT, POST_DEPLOYMENT, START_SERVICE, STOP_SERVICE, DEPLOY_SERVICE, ENABLE_SERVICE,
          DISABLE_SERVICE, CONTAINER_SETUP, CONTAINER_DEPLOY, WRAP_UP),
      ORCHESTRATION_STENCILS, COMMON),

  /**
   * App dynamics state type.
   */
  APP_DYNAMICS(AppDynamicsState.class, VERIFICATIONS, 2, asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * New relic state type.
   */
  NEW_RELIC(NewRelicState.class, VERIFICATIONS, 3, "New Relic", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * New relic deployment marker state type.
   */
  NEW_RELIC_DEPLOYMENT_MARKER(NewRelicDeploymentMarkerState.class, VERIFICATIONS, 4, "New Relic Deployment Marker",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * dyna trace state type.
   */
  DYNA_TRACE(DynatraceState.class, VERIFICATIONS, 5, "Dynatrace", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Prometheus state type.
   */
  PROMETHEUS(PrometheusState.class, VERIFICATIONS, 6, asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Splunk V2 state type.
   */
  SPLUNKV2(SplunkV2State.class, VERIFICATIONS, 8, "SPLUNK", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Elk state type.
   */
  ELK(ElkAnalysisState.class, VERIFICATIONS, 9, "ELK", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * LOGZ state type.
   */
  LOGZ(LogzAnalysisState.class, VERIFICATIONS, 10, "LOGZ", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Sumo state type.
   */
  SUMO(SumoLogicAnalysisState.class, VERIFICATIONS, 11, "Sumo Logic",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * Sumo state type.
   */
  DATA_DOG(DatadogState.class, VERIFICATIONS, 12, "Datadog Metrics",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * DatadogLog state type.
   */
  DATA_DOG_LOG(DatadogLogState.class, VERIFICATIONS, 13, "Datadog Log",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),
  // CVNG step needs to be removed. Keeping it so that the old execution that have CVNG step do not fail with error.
  // Our workflow execution TTL is 6 months so we can remove this after 6 months.
  @Deprecated
  CVNG(CVNGState.class, VERIFICATIONS, 13, "CVNG verification", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Cloud watch state type.
   */
  CLOUD_WATCH(CloudWatchState.class, VERIFICATIONS, 14, "CloudWatch",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  AWS_LAMBDA_VERIFICATION(
      AwsLambdaVerification.class, VERIFICATIONS, 15, "AWS Lambda Verification", asList(), ORCHESTRATION_STENCILS),

  /**
   * Generic APM verification state type.
   */
  APM_VERIFICATION(APMVerificationState.class, VERIFICATIONS, 16, "APM Verification",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**

   * Generic LOG verification state type.
   */
  LOG_VERIFICATION(CustomLogVerificationState.class, VERIFICATIONS, 17, "Log Verification",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * Bugsnag verification state type.
   */
  BUG_SNAG(BugsnagState.class, VERIFICATIONS, 18, "Bugsnag", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * StackDriver state type.
   */
  STACK_DRIVER(StackDriverState.class, VERIFICATIONS, 19, "Stackdriver",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * StackDriver log state type.
   */
  STACK_DRIVER_LOG(StackDriverLogState.class, VERIFICATIONS, 20, "Stackdriver Log",
      asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP), ORCHESTRATION_STENCILS),

  /**
   * Instana state type
   */
  INSTANA(InstanaState.class, VERIFICATIONS, 21, "Instana", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Scalyr state type
   */
  SCALYR(ScalyrState.class, VERIFICATIONS, 22, "Scalyr", asList(K8S_PHASE_STEP, CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS),

  /**
   * Env state state type.
   */
  ENV_STATE(EnvState.class, ENVIRONMENTS, asList(), PIPELINE_STENCILS),

  ENV_LOOP_STATE(EnvLoopState.class, ENVIRONMENTS, asList(), NONE),

  /**
   * Env state state type.
   */
  ENV_RESUME_STATE(EnvResumeState.class, ENVIRONMENTS, emptyList(), NONE),
  ENV_LOOP_RESUME_STATE(EnvLoopResumeState.class, ENVIRONMENTS, emptyList(), NONE),

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

  APPROVAL_RESUME(ApprovalResumeState.class, OTHERS, emptyList(), NONE),

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
   * GoogleCloudBuild state type.
   */
  GCB(GcbState.class, OTHERS, "Google Cloud Build", emptyList(), emptyList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Bamboo state type
   */
  BAMBOO(BambooState.class, OTHERS, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Artifact Collection state type.
   */
  ARTIFACT_COLLECTION(ArtifactCollectionState.class, COLLECTIONS, ARTIFACT_COLLECTION_STEP, emptyList(), emptyList(),
      ORCHESTRATION_STENCILS, COMMON),

  /**
   * Artifact Collection state type.
   */
  ARTIFACT_CHECK(ArtifactCheckState.class, OTHERS, 4, "Artifact Check", asList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  AZURE_NODE_SELECT(AzureNodeSelectState.class, CLOUD, "Azure Select Nodes",
      Lists.newArrayList(InfrastructureMappingType.AZURE_INFRA, InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM),
      asList(INFRASTRUCTURE_NODE, SELECT_NODE), ORCHESTRATION_STENCILS),

  /**
   * Azure Virtual Machine Scale Set setup states.
   */
  AZURE_VMSS_SETUP(AzureVMSSSetupState.class, AZURE_VMSS, WorkflowServiceHelper.AZURE_VMSS_SETUP,
      Lists.newArrayList(InfrastructureMappingType.AZURE_VMSS), asList(PhaseStepType.AZURE_VMSS_SETUP),
      ORCHESTRATION_STENCILS),

  AZURE_VMSS_DEPLOY(AzureVMSSDeployState.class, AZURE_VMSS, WorkflowServiceHelper.AZURE_VMSS_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.AZURE_VMSS), asList(PhaseStepType.AZURE_VMSS_DEPLOY),
      ORCHESTRATION_STENCILS),

  AZURE_VMSS_ROLLBACK(AzureVMSSRollbackState.class, AZURE_VMSS, WorkflowServiceHelper.AZURE_VMSS_ROLLBACK,
      asList(InfrastructureMappingType.AZURE_VMSS), asList(PhaseStepType.AZURE_VMSS_ROLLBACK), ORCHESTRATION_STENCILS),

  AZURE_VMSS_SWITCH_ROUTES(AzureVMSSSwitchRoutesState.class, AZURE_VMSS, WorkflowServiceHelper.AZURE_VMSS_SWITCH_ROUTES,
      singletonList(InfrastructureMappingType.AZURE_VMSS), singletonList(PhaseStepType.AZURE_VMSS_SWITCH_ROUTES),
      ORCHESTRATION_STENCILS),

  AZURE_VMSS_SWITCH_ROUTES_ROLLBACK(AzureVMSSSwitchRoutesRollbackState.class, AZURE_VMSS,
      WorkflowServiceHelper.AZURE_VMSS_SWITCH_ROUTES_ROLLBACK, singletonList(InfrastructureMappingType.AZURE_VMSS),
      singletonList(PhaseStepType.AZURE_VMSS_SWITCH_ROLLBACK), ORCHESTRATION_STENCILS),

  /**
   * Azure Web App states
   */
  AZURE_WEBAPP_SLOT_SETUP(AzureWebAppSlotSetup.class, AZURE_WEBAPP, WorkflowServiceHelper.AZURE_WEBAPP_SLOT_SETUP,
      Lists.newArrayList(InfrastructureMappingType.AZURE_WEBAPP), singletonList(PhaseStepType.AZURE_WEBAPP_SLOT_SETUP),
      ORCHESTRATION_STENCILS),

  AZURE_WEBAPP_SLOT_SWAP(AzureWebAppSlotSwap.class, AZURE_WEBAPP, WorkflowServiceHelper.AZURE_WEBAPP_SLOT_SWAP,
      Lists.newArrayList(InfrastructureMappingType.AZURE_WEBAPP), singletonList(PhaseStepType.AZURE_WEBAPP_SLOT_SWAP),
      ORCHESTRATION_STENCILS),

  AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC(AzureWebAppSlotShiftTraffic.class, AZURE_WEBAPP,
      WorkflowServiceHelper.AZURE_WEBAPP_SLOT_TRAFFIC_SHIFT, Lists.newArrayList(InfrastructureMappingType.AZURE_WEBAPP),
      singletonList(PhaseStepType.AZURE_WEBAPP_SLOT_TRAFFIC_SHIFT), ORCHESTRATION_STENCILS),

  AZURE_WEBAPP_SLOT_ROLLBACK(AzureWebAppSlotRollback.class, AZURE_WEBAPP,
      WorkflowServiceHelper.AZURE_WEBAPP_SLOT_ROLLBACK, Lists.newArrayList(InfrastructureMappingType.AZURE_WEBAPP),
      singletonList(PhaseStepType.AZURE_WEBAPP_SLOT_ROLLBACK), ORCHESTRATION_STENCILS),

  /**
   * AWS Node Select state.
   */
  AWS_NODE_SELECT(AwsNodeSelectState.class, CLOUD, "AWS Select Nodes",
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH), asList(INFRASTRUCTURE_NODE, SELECT_NODE),
      ORCHESTRATION_STENCILS),

  DC_NODE_SELECT(DcNodeSelectState.class, CLOUD, "Select Nodes",
      Lists.newArrayList(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH), asList(SELECT_NODE),
      ORCHESTRATION_STENCILS),

  ROLLING_NODE_SELECT(RollingNodeSelectState.class, CLOUD, "Rolling Select Nodes", asList(), asList(SELECT_NODE),
      ORCHESTRATION_STENCILS),

  PHASE(PhaseSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  PHASE_STEP(PhaseStepSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  STAGING_ORIGINAL_EXECUTION(
      StagingOriginalExecution.class, StencilCategory.STAGING_ORIGINAL_EXECUTION, asList(), NONE),

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
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(AMI_AUTOSCALING_GROUP_SETUP),
      ORCHESTRATION_STENCILS),

  ASG_AMI_SERVICE_ALB_SHIFT_SETUP(AwsAmiServiceTrafficShiftAlbSetup.class, CLOUD,
      WorkflowServiceHelper.ASG_AMI_ALB_SHIFT_SETUP, Lists.newArrayList(InfrastructureMappingType.AWS_AMI),
      asList(AMI_AUTOSCALING_GROUP_SETUP), ORCHESTRATION_STENCILS),

  AWS_AMI_SERVICE_DEPLOY(AwsAmiServiceDeployState.class, COMMANDS, UPGRADE_AUTOSCALING_GROUP,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(AMI_DEPLOY_AUTOSCALING_GROUP),
      ORCHESTRATION_STENCILS),

  ASG_AMI_SERVICE_ALB_SHIFT_DEPLOY(AwsAmiServiceTrafficShiftAlbDeployState.class, COMMANDS,
      WorkflowServiceHelper.ASG_AMI_ALB_SHIFT_DEPLOY, Lists.newArrayList(InfrastructureMappingType.AWS_AMI),
      asList(AMI_DEPLOY_AUTOSCALING_GROUP), ORCHESTRATION_STENCILS),

  AWS_AMI_SWITCH_ROUTES(AwsAmiSwitchRoutesState.class, FLOW_CONTROLS, UPGRADE_AUTOSCALING_GROUP_ROUTE,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), singletonList(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES),
      ORCHESTRATION_STENCILS),

  ASG_AMI_ALB_SHIFT_SWITCH_ROUTES(AwsAmiTrafficShiftAlbSwitchRoutesState.class, FLOW_CONTROLS,
      SPOTINST_ALB_SHIFT_LISTENER_UPDATE, Lists.newArrayList(InfrastructureMappingType.AWS_AMI),
      singletonList(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES), ORCHESTRATION_STENCILS),

  AWS_AMI_ROLLBACK_SWITCH_ROUTES(AwsAmiRollbackSwitchRoutesState.class, FLOW_CONTROLS, ROLLBACK_AUTOSCALING_GROUP_ROUTE,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), singletonList(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES),
      ORCHESTRATION_STENCILS),

  ASG_AMI_ROLLBACK_ALB_SHIFT_SWITCH_ROUTES(AwsAmiRollbackTrafficShiftAlbSwitchRoutesState.class, FLOW_CONTROLS,
      ROLLBACK_AUTOSCALING_GROUP_ROUTE, Lists.newArrayList(InfrastructureMappingType.AWS_AMI),
      singletonList(AMI_SWITCH_AUTOSCALING_GROUP_ROUTES), ORCHESTRATION_STENCILS),

  AWS_AMI_SERVICE_ROLLBACK(AwsAmiServiceRollback.class, COMMANDS, ROLLBACK_AWS_AMI_CLUSTER,
      Lists.newArrayList(InfrastructureMappingType.AWS_AMI), asList(AMI_DEPLOY_AUTOSCALING_GROUP),
      ORCHESTRATION_STENCILS),

  ECS_SERVICE_SETUP(EcsServiceSetup.class, ECS, WorkflowServiceHelper.ECS_SERVICE_SETUP, Lists.newArrayList(AWS_ECS),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  SPOTINST_SETUP(SpotInstServiceSetup.class, SPOTINST, WorkflowServiceHelper.SPOTINST_SETUP,
      asList(InfrastructureMappingType.AWS_AMI), asList(PhaseStepType.SPOTINST_SETUP), ORCHESTRATION_STENCILS),

  SPOTINST_ALB_SHIFT_SETUP(SpotinstTrafficShiftAlbSetupState.class, SPOTINST,
      WorkflowServiceHelper.SPOTINST_ALB_SHIFT_SETUP, asList(InfrastructureMappingType.AWS_AMI),
      asList(PhaseStepType.SPOTINST_SETUP), ORCHESTRATION_STENCILS),

  SPOTINST_DEPLOY(SpotInstDeployState.class, SPOTINST, WorkflowServiceHelper.SPOTINST_DEPLOY,
      asList(InfrastructureMappingType.AWS_AMI), asList(PhaseStepType.SPOTINST_DEPLOY), ORCHESTRATION_STENCILS),

  SPOTINST_ALB_SHIFT_DEPLOY(SpotinstTrafficShiftAlbDeployState.class, SPOTINST, WorkflowServiceHelper.SPOTINST_DEPLOY,
      asList(InfrastructureMappingType.AWS_AMI), asList(PhaseStepType.SPOTINST_DEPLOY), ORCHESTRATION_STENCILS),

  SPOTINST_LISTENER_UPDATE(SpotInstListenerUpdateState.class, SPOTINST, WorkflowServiceHelper.SPOTINST_LISTENER_UPDATE,
      asList(InfrastructureMappingType.AWS_AMI), asList(PhaseStepType.SPOTINST_LISTENER_UPDATE),
      ORCHESTRATION_STENCILS),

  SPOTINST_LISTENER_ALB_SHIFT(SpotinstTrafficShiftAlbSwitchRoutesState.class, SPOTINST,
      WorkflowServiceHelper.SPOTINST_LISTENER_UPDATE, asList(InfrastructureMappingType.AWS_AMI),
      asList(PhaseStepType.SPOTINST_LISTENER_UPDATE), ORCHESTRATION_STENCILS),

  SPOTINST_ROLLBACK(SpotInstRollbackState.class, SPOTINST, WorkflowServiceHelper.SPOTINST_ROLLBACK,
      asList(InfrastructureMappingType.AWS_AMI), asList(PhaseStepType.SPOTINST_ROLLBACK), ORCHESTRATION_STENCILS),

  SPOTINST_LISTENER_UPDATE_ROLLBACK(SpotInstListenerUpdateRollbackState.class, SPOTINST,
      WorkflowServiceHelper.SPOTINST_LISTENER_UPDATE_ROLLBACK, asList(InfrastructureMappingType.AWS_AMI),
      asList(PhaseStepType.SPOTINST_LISTENER_UPDATE_ROLLBACK), ORCHESTRATION_STENCILS),

  SPOTINST_LISTENER_ALB_SHIFT_ROLLBACK(SpotinstTrafficShiftAlbRollbackSwitchRoutesState.class, SPOTINST,
      WorkflowServiceHelper.SPOTINST_LISTENER_UPDATE_ROLLBACK, asList(InfrastructureMappingType.AWS_AMI),
      asList(PhaseStepType.SPOTINST_LISTENER_UPDATE_ROLLBACK), ORCHESTRATION_STENCILS),

  ECS_SERVICE_SETUP_ROLLBACK(EcsSetupRollback.class, ECS, StateType.ROLLBACK_ECS_SETUP, Lists.newArrayList(AWS_ECS),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  ECS_DAEMON_SERVICE_SETUP(EcsDaemonServiceSetup.class, ECS, WorkflowServiceHelper.ECS_DAEMON_SERVICE_SETUP,
      Lists.newArrayList(AWS_ECS), asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  ECS_BG_SERVICE_SETUP(EcsBlueGreenServiceSetup.class, ECS, WorkflowServiceHelper.ECS_BG_SERVICE_SETUP_ELB,
      Lists.newArrayList(AWS_ECS), asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  ECS_BG_SERVICE_SETUP_ROUTE53(EcsBlueGreenServiceSetupRoute53DNS.class, ECS,
      WorkflowServiceHelper.ECS_BG_SERVICE_SETUP_ROUTE_53, Lists.newArrayList(AWS_ECS), asList(CONTAINER_SETUP),
      ORCHESTRATION_STENCILS),

  ECS_SERVICE_DEPLOY(EcsServiceDeploy.class, ECS, "ECS Upgrade Containers", Lists.newArrayList(AWS_ECS),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_SERVICE_ROLLBACK(EcsServiceRollback.class, ECS, "ECS Rollback Containers", Lists.newArrayList(AWS_ECS),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_LISTENER_UPDATE(EcsBGUpdateListnerState.class, ECS, ECS_SWAP_TARGET_GROUPS, Lists.newArrayList(AWS_ECS),
      singletonList(ECS_UPDATE_LISTENER_BG), ORCHESTRATION_STENCILS),

  ECS_LISTENER_UPDATE_ROLLBACK(EcsBGUpdateListnerRollbackState.class, ECS, ECS_SWAP_TARGET_GROUPS_ROLLBACK,
      Lists.newArrayList(AWS_ECS), singletonList(ECS_UPDATE_LISTENER_BG), ORCHESTRATION_STENCILS),

  ECS_ROUTE53_DNS_WEIGHT_UPDATE(EcsBGUpdateRoute53DNSWeightState.class, ECS, ECS_ROUTE53_DNS_WEIGHTS,
      Lists.newArrayList(AWS_ECS), singletonList(ECS_UPDATE_ROUTE_53_DNS_WEIGHT), ORCHESTRATION_STENCILS),

  ECS_ROUTE53_DNS_WEIGHT_UPDATE_ROLLBACK(EcsBGRollbackRoute53DNSWeightState.class, ECS,
      ROLLBACK_ECS_ROUTE53_DNS_WEIGHTS, Lists.newArrayList(AWS_ECS), singletonList(ECS_UPDATE_ROUTE_53_DNS_WEIGHT),
      ORCHESTRATION_STENCILS),

  KUBERNETES_SETUP(KubernetesSetup.class, CLOUD, KUBERNETES_SERVICE_SETUP,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  KUBERNETES_SETUP_ROLLBACK(KubernetesSetupRollback.class, COMMANDS, ROLLBACK_KUBERNETES_SETUP,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  KUBERNETES_DEPLOY(KubernetesDeploy.class, COMMANDS, "Kubernetes Upgrade Containers",
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_DEPLOY, WRAP_UP), ORCHESTRATION_STENCILS),

  KUBERNETES_DEPLOY_ROLLBACK(KubernetesDeployRollback.class, COMMANDS, "Kubernetes Rollback Containers",
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  KUBERNETES_STEADY_STATE_CHECK(KubernetesSteadyStateCheck.class, COMMANDS,
      KubernetesSteadyStateCheck.KUBERNETES_STEADY_STATE_CHECK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_STEADY_STATE_CHECK(EcsSteadyStateCheck.class, ECS, AwsConstants.ECS_STEADY_STATE_CHECK,
      Lists.newArrayList(AWS_ECS), asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_RUN_TASK(EcsRunTaskDeploy.class, ECS, AwsConstants.ECS_RUN_TASK, Lists.newArrayList(AWS_ECS),
      asList(CONTAINER_DEPLOY, CONTAINER_SETUP, ECS_UPDATE_LISTENER_BG, ECS_UPDATE_ROUTE_53_DNS_WEIGHT),
      ORCHESTRATION_STENCILS),

  GCP_CLUSTER_SETUP(GcpClusterSetup.class, CLOUD,
      Lists.newArrayList(InfrastructureMappingType.GCP_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES),
      asList(CLUSTER_SETUP), ORCHESTRATION_STENCILS),

  HELM_DEPLOY(HelmDeployState.class, COMMANDS, WorkflowServiceHelper.HELM_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(PhaseStepType.HELM_DEPLOY), ORCHESTRATION_STENCILS),
  HELM_ROLLBACK(HelmRollbackState.class, COMMANDS, WorkflowServiceHelper.HELM_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(PhaseStepType.HELM_DEPLOY), ORCHESTRATION_STENCILS),

  PCF_SETUP(PcfSetupState.class, COMMANDS, WorkflowServiceHelper.PCF_SETUP,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_SETUP), ORCHESTRATION_STENCILS),

  PCF_RESIZE(PcfDeployState.class, COMMANDS, WorkflowServiceHelper.PCF_RESIZE,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  PCF_ROLLBACK(PcfRollbackState.class, COMMANDS, WorkflowServiceHelper.PCF_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  PCF_MAP_ROUTE(MapRouteState.class, FLOW_CONTROLS, StateType.PCF_MAP_ROUTE_NAME,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),

  PCF_UNMAP_ROUTE(UnmapRouteState.class, FLOW_CONTROLS, StateType.PCF_UNMAP_ROUTE_NAME,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_RESIZE), ORCHESTRATION_STENCILS),
  PCF_BG_MAP_ROUTE(PcfSwitchBlueGreenRoutes.class, FLOW_CONTROLS, WorkflowServiceHelper.PCF_BG_MAP_ROUTE,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_SWICH_ROUTES),
      ORCHESTRATION_STENCILS),

  // todo @rk: verify
  PCF_PLUGIN(PcfPluginState.class, COMMANDS, WorkflowServiceHelper.PCF_PLUGIN,
      Lists.newArrayList(InfrastructureMappingType.PCF_PCF), asList(PhaseStepType.PCF_SETUP, PhaseStepType.PCF_RESIZE),
      ORCHESTRATION_STENCILS),

  TERRAFORM_PROVISION(ApplyTerraformProvisionState.class, PROVISIONERS, 0, "Terraform Provision",
      asList(InfrastructureMappingType.AWS_SSH), asList(PRE_DEPLOYMENT, PROVISION_INFRASTRUCTURE),
      ORCHESTRATION_STENCILS),

  TERRAFORM_APPLY(ApplyTerraformState.class, OTHERS, 5, "Terraform Apply", asList(), ORCHESTRATION_STENCILS, COMMON),

  TERRAGRUNT_PROVISION(TerragruntApplyState.class, PROVISIONERS, 0, PROVISION_TERRAGRUNT_NAME,
      asList(InfrastructureMappingType.AWS_SSH), asList(PRE_DEPLOYMENT, PROVISION_INFRASTRUCTURE),
      ORCHESTRATION_STENCILS),

  TERRAGRUNT_DESTROY(TerragruntDestroyState.class, PROVISIONERS, 0, DESTROY_TERRAGRUNT_NAME,
      asList(InfrastructureMappingType.AWS_SSH), asList(POST_DEPLOYMENT, WRAP_UP), ORCHESTRATION_STENCILS),

  TERRAGRUNT_ROLLBACK(TerragruntRollbackState.class, PROVISIONERS, ROLLBACK_TERRAGRUNT_NAME,
      singletonList(InfrastructureMappingType.AWS_SSH), singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  ARM_CREATE_RESOURCE(ARMProvisionState.class, PROVISIONERS, 0, WorkflowServiceHelper.ARM_CREATE_RESOURCE,
      asList(InfrastructureMappingType.AZURE_INFRA, InfrastructureMappingType.AZURE_VMSS,
          InfrastructureMappingType.AZURE_WEBAPP),
      asList(PRE_DEPLOYMENT, PROVISION_INFRASTRUCTURE), ORCHESTRATION_STENCILS),

  ARM_ROLLBACK(ARMRollbackState.class, PROVISIONERS, ProvisionerConstants.ARM_ROLLBACK,
      asList(InfrastructureMappingType.AZURE_INFRA, InfrastructureMappingType.AZURE_VMSS,
          InfrastructureMappingType.AZURE_WEBAPP),
      singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  SHELL_SCRIPT_PROVISION(ShellScriptProvisionState.class, PROVISIONERS, 2, PROVISION_SHELL_SCRIPT,
      asList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  TERRAFORM_DESTROY(DestroyTerraformProvisionState.class, PROVISIONERS, 0, "Terraform Destroy",
      asList(InfrastructureMappingType.AWS_SSH), asList(POST_DEPLOYMENT, WRAP_UP), ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_CREATE_STACK(CloudFormationCreateStackState.class, PROVISIONERS, 1, PROVISION_CLOUD_FORMATION,
      asList(InfrastructureMappingType.AWS_SSH), asList(PRE_DEPLOYMENT, PROVISION_INFRASTRUCTURE),
      ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_DELETE_STACK(CloudFormationDeleteStackState.class, PROVISIONERS, DE_PROVISION_CLOUD_FORMATION,
      asList(InfrastructureMappingType.AWS_SSH), asList(POST_DEPLOYMENT, WRAP_UP), ORCHESTRATION_STENCILS),

  KUBERNETES_SWAP_SERVICE_SELECTORS(KubernetesSwapServiceSelectors.class, KUBERNETES, 2,
      Constants.KUBERNETES_SWAP_SERVICE_SELECTORS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.AZURE_KUBERNETES,
          InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY, ROUTE_UPDATE, WRAP_UP, K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  CLOUD_FORMATION_ROLLBACK_STACK(CloudFormationRollbackStackState.class, PROVISIONERS, ROLLBACK_CLOUD_FORMATION,
      singletonList(InfrastructureMappingType.AWS_SSH), singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  TERRAFORM_ROLLBACK(TerraformRollbackState.class, PROVISIONERS, ROLLBACK_TERRAFORM_NAME,
      singletonList(InfrastructureMappingType.AWS_SSH), singletonList(PRE_DEPLOYMENT), ORCHESTRATION_STENCILS),

  K8S_DEPLOYMENT_ROLLING(K8sRollingDeploy.class, KUBERNETES, 3, WorkflowConstants.K8S_DEPLOYMENT_ROLLING,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_SCALE(K8sScale.class, KUBERNETES, 6, WorkflowConstants.K8S_SCALE,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_DEPLOYMENT_ROLLING_ROLLBACK(K8sRollingDeployRollback.class, KUBERNETES, 4,
      WorkflowConstants.K8S_DEPLOYMENT_ROLLING_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_BLUE_GREEN_DEPLOY(K8sBlueGreenDeploy.class, KUBERNETES, 1, WorkflowConstants.K8S_BLUE_GREEN_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_CANARY_DEPLOY(K8sCanaryDeploy.class, KUBERNETES, 0, WorkflowConstants.K8S_CANARY_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_DELETE(K8sDelete.class, KUBERNETES, 7, WorkflowConstants.K8S_DELETE,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  RANCHER_RESOLVE(RancherResolveState.class, KUBERNETES, 30, WorkflowConstants.RANCHER_RESOLVE_CLUSTERS,
      Lists.newArrayList(InfrastructureMappingType.RANCHER_KUBERNETES), asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  RANCHER_K8S_DEPLOYMENT_ROLLING(RancherK8sRollingDeploy.class, KUBERNETES, 31,
      WorkflowConstants.RANCHER_K8S_DEPLOYMENT_ROLLING,
      Lists.newArrayList(InfrastructureMappingType.RANCHER_KUBERNETES), asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  RANCHER_K8S_CANARY_DEPLOY(RancherK8sCanaryDeploy.class, KUBERNETES, 32, WorkflowConstants.RANCHER_K8S_CANARY_DEPLOY,
      Lists.newArrayList(InfrastructureMappingType.RANCHER_KUBERNETES), asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  RANCHER_K8S_DELETE(K8sDelete.class, KUBERNETES, 38, WorkflowConstants.RANCHER_K8S_DELETE,
      Lists.newArrayList(InfrastructureMappingType.RANCHER_KUBERNETES), asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK(RancherK8sRollingDeployRollback.class, KUBERNETES, 39,
      WorkflowConstants.RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK,
      Lists.newArrayList(InfrastructureMappingType.RANCHER_KUBERNETES), asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  JIRA_CREATE_UPDATE(JiraCreateUpdate.class, COLLABORATION, 1, "Jira",
      asList(PRE_DEPLOYMENT, POST_DEPLOYMENT, START_SERVICE, STOP_SERVICE, DEPLOY_SERVICE, ENABLE_SERVICE,
          DISABLE_SERVICE, CONTAINER_SETUP, CONTAINER_DEPLOY, WRAP_UP),
      ORCHESTRATION_STENCILS, COMMON),

  SERVICENOW_CREATE_UPDATE(ServiceNowCreateUpdateState.class, COLLABORATION, 3, "ServiceNow",
      asList(PRE_DEPLOYMENT, POST_DEPLOYMENT, START_SERVICE, STOP_SERVICE, DEPLOY_SERVICE, ENABLE_SERVICE,
          DISABLE_SERVICE, CONTAINER_SETUP, CONTAINER_DEPLOY, WRAP_UP),
      ORCHESTRATION_STENCILS, COMMON),

  K8S_TRAFFIC_SPLIT(K8sTrafficSplitState.class, KUBERNETES, 8, K8S_TRAFFIC_SPLIT_STATE_NAME,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  K8S_APPLY(K8sApplyState.class, KUBERNETES, 5, K8S_APPLY_STATE,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES,
          InfrastructureMappingType.AZURE_KUBERNETES),
      asList(K8S_PHASE_STEP), ORCHESTRATION_STENCILS),

  CUSTOM_DEPLOYMENT_FETCH_INSTANCES(InstanceFetchState.class, OTHERS, 5, WorkflowServiceHelper.FETCH_INSTANCES,
      Lists.newArrayList(InfrastructureMappingType.CUSTOM), asList(PhaseStepType.CUSTOM_DEPLOYMENT_PHASE_STEP),
      ORCHESTRATION_STENCILS);

  private static final String PCF_MAP_ROUTE_NAME = "Map Route";
  private static final String PCF_UNMAP_ROUTE_NAME = "Unmap Route";
  private static final String ROLLBACK_ECS_SETUP = "Rollback ECS Setup";
  private static final String stencilsPath = "/templates/stencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";

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
    this.phaseStepTypes = phaseStepTypes.stream().map(Enum::name).collect(toList());
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
      throw new UnexpectedException("Error in initializing StateType-" + file, exception);
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
      return (stencilCategory != COMMANDS && stencilCategory != CLOUD && stencilCategory != ECS)
          || supportedInfrastructureMappingTypes.contains(infrastructureMappingType);
    } else if (context instanceof InfrastructureDefinition) {
      InfrastructureDefinition infrastructureDefinition = (InfrastructureDefinition) context;
      InfrastructureMappingType infrastructureMappingType =
          InfrastructureMappingType.valueOf(infrastructureDefinition.getInfrastructure().getInfrastructureType());
      return (stencilCategory != COMMANDS && stencilCategory != CLOUD && stencilCategory != ECS)
          || supportedInfrastructureMappingTypes.contains(infrastructureMappingType);
    } else {
      DeploymentType deploymentType = (DeploymentType) context;
      List<DeploymentType> supportedDeploymentTypes = new ArrayList<>();
      // TODO: SSH deployment referenced by more than one SelectNode states
      if (supportedInfrastructureMappingTypes != null) {
        for (InfrastructureMappingType supportedInfrastructureMappingType : supportedInfrastructureMappingTypes) {
          supportedDeploymentTypes.addAll(supportedInfrastructureMappingType.getDeploymentTypes());
        }
      }
      return (stencilCategory != COMMANDS && stencilCategory != CLOUD)
          || supportedDeploymentTypes.contains(deploymentType);
    }
  }

  @Override
  public List<String> getPhaseStepTypes() {
    return phaseStepTypes;
  }

  public boolean isVerificationState() {
    return this.stencilCategory == StencilCategory.VERIFICATIONS;
  }
}