package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface EntityTypeConstants {
  String PROJECTS = "Projects";
  String PIPELINES = "Pipelines";
  String PIPELINE_STEPS = "PipelineSteps";
  String CONNECTORS = "Connectors";
  String SECRETS = "Secrets";
  String SERVICE = "Service";
  String ENVIRONMENT = "Environment";
  String INPUT_SETS = "InputSets";
  String CV_CONFIG = "CvConfig";
  String DELEGATES = "Delegates";
  String DELEGATE_CONFIGURATIONS = "DelegateConfigurations";
  String CV_VERIFICATION_JOB = "CvVerificationJob";
  String INTEGRATION_STAGE = "IntegrationStage";
  String INTEGRATION_STEPS = "IntegrationSteps";
  String CV_KUBERNETES_ACTIVITY_SOURCE = "CvKubernetesActivitySource";
  String DEPLOYMENT_STEPS = "DeploymentSteps";
  String DEPLOYMENT_STAGE = "DeploymentStage";
  String APPROVAL_STAGE = "ApprovalStage";
  String FEATURE_FLAG_STAGE = "FeatureFlagStage";
  String TRIGGERS = "Triggers";
  String MONITORED_SERVICE = "MonitoredService";
  String TEMPLATE = "Template";
  String GIT_REPOSITORIES = "GitRepositories";
  String FEATURE_FLAGS = "FeatureFlags";
  String HTTP = "Http";
  String JIRA_CREATE = "JiraCreate";
  String SHELL_SCRIPT = "ShellScript";
  String K8S_CANARY_DEPLOY = "K8sCanaryDeploy";
  String K8S_APPLY = "K8sApply";
  String K8S_BLUE_GREEN_DEPLOY = "K8sBlueGreenDeploy";
  String K8S_ROLLING_DEPLOY = "K8sRollingDeploy";
  String K8S_ROLLING_ROLLBACK = "K8sRollingRollback";
  String K8S_SCALE = "K8sScale";
  String K8S_DELETE = "K8sDelete";
  String K8S_BG_SWAP_SERVICES = "K8sBGSwapServices";
  String K8S_CANARY_DELETE = "K8sCanaryDelete";
  String TERRAFORM_APPLY = "TerraformApply";
  String TERRAFORM_PLAN = "TerraformPlan";
  String SERVICENOW_APPROVAL = "ServiceNowApproval";
}
