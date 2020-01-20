package software.wings.common;

import java.util.regex.Pattern;

/**
 * Common constants across application.
 */
// TODO: Do not use one centralized place for different semantics. Instead define your constants in the context they
//       make sense. When the proper layering is in place well located constant will be acceptable everywhere it is
//       needed.
@Deprecated
public interface Constants {
  /**
   * The constant COLLECT_ARTIFACT.
   */
  String COLLECT_ARTIFACT = "Collect Artifact";

  /**
   * The constant COLLECT_ARTIFACT.
   */
  String ARTIFACT_COLLECTION = "Artifact Collection";

  /**
   * The constant AMI setup commands
   */
  String AMI_SETUP_COMMAND_NAME = "AMI Service Setup";

  /**
   * The constant KUBERNETES_STEADY_STATE_CHECK.
   */
  String KUBERNETES_STEADY_STATE_CHECK = "Steady State Check";

  String ECS_STEADY_STATE_CHECK = "Ecs Steady State Check";
  /**
   * The constant KUBERNETES_SWAP_SERVICE_SELECTORS.
   */
  String KUBERNETES_SWAP_SERVICE_SELECTORS = "Swap Service Selectors";
  /**
   * The constant SUB_WORKFLOW_ID.
   */
  String SUB_WORKFLOW_ID = "subWorkflowId";
  /**
   * The constant SUB_WORKFLOW.
   */
  String SUB_WORKFLOW = "SUB_WORKFLOW";
  /**
   * The constant PHASE_NAME_PREFIX.
   */
  String PHASE_NAME_PREFIX = "Phase ";

  /**
   * The constant phaseNamePattern.
   */
  Pattern phaseNamePattern = Pattern.compile("Phase [0-9]+");

  /**
   * The constant PROVISION_NODE_NAME.
   */
  @Deprecated String PROVISION_NODE_NAME = "Provision Nodes";

  String ROLLBACK_TERRAFORM_NAME = "Terraform Rollback";

  /**
   * The constant STEP_VALIDATION_MESSAGE.
   */
  String STEP_VALIDATION_MESSAGE = "Some fields %s are found to be invalid/incomplete.";
  /**
   * The constant PHASE_STEP_VALIDATION_MESSAGE.
   */
  String PHASE_STEP_VALIDATION_MESSAGE = "Some steps %s are found to be invalid/incomplete.";
  /**
   * The constant WORKFLOW_VALIDATION_MESSAGE.
   */
  String WORKFLOW_VALIDATION_MESSAGE = "Some phases/steps %s are found to be invalid/incomplete.";

  /**
   * The constant WORKFLOW_ENV_INFRAMAPPING_VALIDATION_MESSAGE.
   */
  String WORKFLOW_ENV_VALIDATION_MESSAGE = "Environment is found to be invalid/incomplete.";

  /**
   * The constant WORKFLOW_ENV_INFRAMAPPING_VALIDATION_MESSAGE.
   */
  String WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE =
      "Some phases %s Service Infrastructure are found to be invalid/incomplete.";

  /**
   * The constant DEFAULT_STATE_TIMEOUT_MILLIS.
   */
  long DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS = 5L * 60L * 1000L; // 5 minutes

  String PROVISION_CLOUD_FORMATION = "CloudFormation Create Stack";
  String DE_PROVISION_CLOUD_FORMATION = "CloudFormation Delete Stack";
  String ROLLBACK_CLOUD_FORMATION = "CloudFormation Rollback Stack";

  String PROVISION_SHELL_SCRIPT = "Shell Script Provision";

  /**
   * The constant DEFAULT_STEADY_STATE_TIMEOUT.
   */
  int DEFAULT_STEADY_STATE_TIMEOUT = 10;

  /**
   * The constant LAST_UPDATED_AT.
   */
  String LAST_UPDATED_AT = "lastUpdatedAt";

  /**
   * Constants for expression
   */
  String ARTIFACT_S3_BUCKET_EXPRESSION = "${artifact.bucketName}";
  /**
   * The constant ARTIFACT__S3_KEY_EXPRESSION.
   */
  String ARTIFACT__S3_KEY_EXPRESSION = "${artifact.key}";

  /**
   * The constant EXECUTE_WITH_PREVIOUS_STEPS.
   */
  String EXECUTE_WITH_PREVIOUS_STEPS = "executeWithPreviousSteps";

  /**
   * The constant ASG_COMMAND_NAME.
   */
  String ASG_COMMAND_NAME = "Resize AutoScaling Group";

  /**
   * The constant HARNESS_NAME.
   */
  String HARNESS_NAME = "Harness";

  String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

  /**
   * The cloudwatch metric url.
   */
  String HARNESS_KUBE_CONFIG_PATH = "HARNESS_KUBE_CONFIG_PATH";

  String PCF_OLD_APP_NAME = "pcfOldAppName";
  String TOKEN_FIELD = "Bearer Token(HTTP Header)";

  String WINDOWS_HOME_DIR = "%USERPROFILE%";

  String ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM = "ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM";

  String DEPLOYMENT_TRIGGERED_BY = "deploymentTriggeredBy";

  /**
   * Quartz job detail key names
   */
  String ACCOUNT_ID_KEY = "accountId";
  String APP_ID_KEY = "appId";
}
