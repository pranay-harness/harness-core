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
   * The constant PHASE_PARAM.
   */
  String PHASE_PARAM = "PHASE_PARAM";

  String K8S_DEPLOYMENT_ROLLING = "Rollout Deployment";
  String K8S_DEPLOYMENT_ROLLING_ROLLBACK = "Rollback Deployment";
  String K8S_BLUE_GREEN_DEPLOY = "Blue/Green Deployment";
  String K8S_SCALE = "Scale";
  String K8S_DELETE = "Delete";
  String K8S_CANARY_DEPLOY = "Canary Deployment";
  String K8S_STAGE_DEPLOY = "Stage Deployment";

  String K8S_PRIMARY_PHASE_NAME = "Primary";
  String K8S_CANARY_PHASE_NAME = "Canary";

  /**
   * The constant COLLECT_ARTIFACT.
   */
  String COLLECT_ARTIFACT = "Collect Artifact";

  /**
   * The constant COLLECT_ARTIFACT.
   */
  String ARTIFACT_COLLECTION = "Artifact Collection";

  /**
   * The constant AWS_LAMBDA_COMMAND_NAME.
   */
  String AWS_LAMBDA_COMMAND_NAME = "Deploy AWS Lambda Function";

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
   * The constant PHASE_VALIDATION_MESSAGE.
   */
  String PHASE_VALIDATION_MESSAGE = "Some steps %s are found to be invalid/incomplete.";
  /**
   * The constant WORKFLOW_VALIDATION_MESSAGE.
   */
  String WORKFLOW_VALIDATION_MESSAGE = "Some phases %s are found to be invalid/incomplete.";

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
   * The constant PIPELINE_ENV_STATE_VALIDATION_MESSAGE.
   */
  String PIPELINE_ENV_STATE_VALIDATION_MESSAGE = "Some workflows %s are found to be invalid/incomplete.";

  /**
   * The constant DEFAULT_STATE_TIMEOUT_MILLIS.
   */
  long DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes

  /**
   * O
   * The constant DEFAULT_PARENT_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_PARENT_STATE_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000; // 4 hours

  /**
   * The constant for Default Env State timeout
   */
  Integer ENV_STATE_TIMEOUT_MILLIS = 12 * 60 * 60 * 1000; // 12 hours

  /**
   * The constant NOT_REGISTERED.
   */
  String NOT_REGISTERED = "<Not registered yet>";

  String PROVISION_CLOUD_FORMATION = "CloudFormation Create Stack";
  String DE_PROVISION_CLOUD_FORMATION = "CloudFormation Delete Stack";
  String ROLLBACK_CLOUD_FORMATION = "CloudFormation Rollback Stack";

  String PROVISION_SHELL_SCRIPT = "Shell Script Provision";

  /**
   * The constant DELEGATE_DIR.
   */
  String DELEGATE_DIR = "harness-delegate";
  String DOCKER_DELEGATE = "harness-delegate-docker";
  String KUBERNETES_DELEGATE = "harness-delegate-kubernetes";
  String ECS_DELEGATE = "harness-delegate-ecs";

  String SELF_DESTRUCT = "[SELF_DESTRUCT]";

  /**
   * The constant MAX_DELEGATE_LAST_HEARTBEAT.
   */
  long MAX_DELEGATE_LAST_HEARTBEAT = (5 * 60 * 1000L) + (15 * 1000L); // 5 minutes 15 seconds

  /**
   * The constant DEFAULT_STEADY_STATE_TIMEOUT.
   */
  int DEFAULT_STEADY_STATE_TIMEOUT = 10;

  /**
   * The constant LAST_UPDATED_AT.
   */
  String LAST_UPDATED_AT = "lastUpdatedAt";
  /**
   * The constant PATH.
   */
  String PATH = "path";
  /**
   * The constant PARENT.
   */
  String PARENT = "parent";

  /**
   * The constant S3.
   */
  String S3 = "S3";

  /**
   * The constant FILE_CONTENT_NOT_STORED.
   */
  String FILE_CONTENT_NOT_STORED = "__FILE_CONTENT_NOT_STORED__";

  /**
   * The constant WINGS_RUNTIME_PATH.
   */
  String WINGS_RUNTIME_PATH = "WINGS_RUNTIME_PATH";
  /**
   * The constant WINGS_STAGING_PATH.
   */
  String WINGS_STAGING_PATH = "WINGS_STAGING_PATH";
  /**
   * The constant WINGS_BACKUP_PATH.
   */
  String WINGS_BACKUP_PATH = "WINGS_BACKUP_PATH";
  /**
   * Constants for HTTP state
   */
  String HTTP_URL = "httpUrl";
  /**
   * The constant HTTP_RESPONSE_METHOD.
   */
  String HTTP_RESPONSE_METHOD = "httpResponseMethod";

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
   * The constant DEFAULT_RUNTIME_PATH.
   */
  String DEFAULT_RUNTIME_PATH = "$HOME/${app.name}/${service.name}/${env.name}/runtime";
  /**
   * The constant DEFAULT_BACKUP_PATH.
   */
  String DEFAULT_BACKUP_PATH = "$HOME/${app.name}/${service.name}/${env.name}/backup/${timestampId}";
  /**
   * The constant DEFAULT_STAGING_PATH.
   */
  String DEFAULT_STAGING_PATH = "$HOME/${app.name}/${service.name}/${env.name}/staging/${timestampId}";
  /**
   * The constant DEFAULT_WINDOWS_RUNTIME_PATH.
   */
  String DEFAULT_WINDOWS_RUNTIME_PATH = "%USERPROFILE%\\${app.name}\\${service.name}\\${env.name}\\runtime";
  /**
   * The constant RUNTIME_PATH.
   */
  String RUNTIME_PATH = "RUNTIME_PATH";
  /**
   * The constant BACKUP_PATH.
   */
  String BACKUP_PATH = "BACKUP_PATH";
  /**
   * The constant STAGING_PATH.
   */
  String STAGING_PATH = "STAGING_PATH";
  /**
   * The constant WINDOWS_RUNTIME_PATH.
   */
  String WINDOWS_RUNTIME_PATH = "WINDOWS_RUNTIME_PATH";

  /**
   * The constant RESUMED_COLOR.
   */
  String RESUMED_COLOR = "#1DAEE2";
  /**
   * The constant COMPLETED_COLOR.
   */
  String COMPLETED_COLOR = "#5CB04D";
  /**
   * The constant FAILED_COLOR.
   */
  String FAILED_COLOR = "#EC372E";
  /**
   * The constant PAUSED_COLOR.
   */
  String PAUSED_COLOR = "#FBB731";
  /**
   * The constant ABORTED_COLOR.
   */
  String ABORTED_COLOR = "#77787B";
  /**
   * The constant WHITE_COLOR.
   */
  String WHITE_COLOR = "#FFFFFF";
  /**
   * The constant LINK_COLOR.
   */
  String LINK_COLOR = "#1A89BF";

  /**
   * The constant HARNESS_NAME.
   */
  String HARNESS_NAME = "Harness";

  String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";
  /**
   * The constant DEFAULT_AWS_HOST_NAME_CONVENTION.
   */
  String DEFAULT_AWS_HOST_NAME_CONVENTION = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";

  /**
   * The cloudwatch metric url.
   */
  String HARNESS_KUBE_CONFIG_PATH = "HARNESS_KUBE_CONFIG_PATH";

  String INFRA_ROUTE = "infra.route";
  String INFRA_TEMP_ROUTE = "infra.tempRoute";

  String PCF_APP_NAME = "pcfAppName";
  String PCF_OLD_APP_NAME = "pcfOldAppName";
  String TOKEN_FIELD = "Bearer Token(HTTP Header)";

  String WINDOWS_HOME_DIR = "%USERPROFILE%";

  String ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM = "ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM";

  String DEPLOYMENT_TRIGGERED_BY = "deploymentTriggeredBy";
  long ARTIFACT_FILE_SIZE_LIMIT = 4L * 1024L * 1024L * 1024L; // 4GB

  long ML_RECORDS_TTL_MONTHS = 6;

  /**
   * Quartz job detail key names
   */
  String ACCOUNT_ID_KEY = "accountId";
  String APP_ID_KEY = "appId";
}
