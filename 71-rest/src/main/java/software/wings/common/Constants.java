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
   * The constant DEFAULT_WORKFLOW_NAME.
   */
  String DEFAULT_WORKFLOW_NAME = "MAIN";

  /**
   * The constant PHASE_PARAM.
   */
  String PHASE_PARAM = "PHASE_PARAM";
  /**
   * The constant SUMMARY_PAYLOAD_LIMIT.
   */
  int SUMMARY_PAYLOAD_LIMIT = 1024;

  /**
   * The constant PRE_DEPLOYMENT.
   */
  String ROLLBACK_PROVISIONERS = "Rollback Provisioners";
  /**
   * The constant DISABLE_SERVICE.
   */
  String DISABLE_SERVICE = "Disable Service";
  /**
   * The constant ENABLE_SERVICE.
   */
  String ENABLE_SERVICE = "Enable Service";
  /**
   * The constant DEPLOY_SERVICE.
   */
  String DEPLOY_SERVICE = "Deploy Service";

  /**
   * The constant ROLLBACK_SERVICE.
   */
  String ROLLBACK_SERVICE = "Rollback Service";

  /**
   * The constant STOP_SERVICE.
   */
  String STOP_SERVICE = "Stop Service";
  /**
   * The constant VERIFY_SERVICE.
   */
  String VERIFY_SERVICE = "Verify Service";

  String VERIFY_STAGING = "Verify Staging";
  /**
   * The constant DEPLOY_CONTAINERS.
   */
  String DEPLOY_CONTAINERS = "Deploy Containers";
  /**
   * The constant SETUP_CONTAINER.
   */
  String SETUP_CONTAINER = "Set up Container";
  /**
   * The constant SETUP_CLUSTER.
   */
  String SETUP_CLUSTER = "Setup Cluster";
  /**
   * The constant ECS_SERVICE_SETUP.
   */
  String ECS_SERVICE_SETUP = "ECS Service Setup";

  String ECS_DAEMON_SERVICE_SETUP = "ECS Daemon Service Setup";

  String ECS_BG_SERVICE_SETUP_ELB = "Setup Load Balancer";

  String ECS_BG_SERVICE_SETUP_ROUTE_53 = "Setup Route 53";

  String ECS_DAEMON_SCHEDULING_STRATEGY = "DAEMON";

  String ECS_REPLICA_SCHEDULING_STRATEGY = "REPLICA";

  String ECS_SERVICE_SPEC = "ECS_SERVICE_SPEC";

  String BASIC = "BASIC";

  String ROLLBACK_ECS_SETUP = "Rollback ECS Setup";

  String SETUP = "Setup";

  String DEPLOY = "Deploy";

  String SCALE = "Scale";

  String VERIFY = "Verify";

  String PCF_SETUP = "App Setup";

  String PCF_RESIZE = "App Resize";

  String ROLLBACK = "Rollback";

  String PCF_ROLLBACK = "App Rollback";

  String PCF_MAP_ROUTE = "Map Route";

  String ECS_SWAP_TARGET_GROUPS = "Swap Target Groups";

  String ECS_ROUTE53_DNS_WEIGHTS = "Swap Route 53 DNS";

  String CHANGE_ROUTE53_DNS_WEIGHTS = "Change Route 53 Weights";

  String ROLLBACK_ECS_ROUTE53_DNS_WEIGHTS = "Rollback Route 53 Weights";

  String ECS_SWAP_TARGET_GROUPS_ROLLBACK = "Rollback Swap Target Groups";

  String PCF_BG_MAP_ROUTE = "Update Route";

  String PCF_BG_SWAP_ROUTE = "Swap Routes";

  String PCF_UNMAP_ROUT = "Unmap Route";

  String K8S_DEPLOYMENT_ROLLING = "Rollout Deployment";
  String K8S_DEPLOYMENT_ROLLING_ROLLBAK = "Rollback Deployment";
  String K8S_BLUE_GREEN_DEPLOY = "Blue/Green Deployment";
  String K8S_SCALE = "Scale";
  String K8S_DELETE = "Delete";
  String K8S_CANARY_DEPLOY = "Canary Deployment";
  String K8S_STAGE_DEPLOY = "Stage Deployment";

  String K8S_PRIMARY_PHASE_NAME = "Primary";
  String K8S_CANARY_PHASE_NAME = "Canary";

  /**
   * The constant KUBERNETES_SERVICE_SETUP.
   */
  String KUBERNETES_SERVICE_SETUP = "Kubernetes Service Setup";
  /**
   * The constant ROLLBACK_KUBERNETES_SETUP.
   */
  String ROLLBACK_KUBERNETES_SETUP = "Rollback Kubernetes Setup";
  /**
   * The constant PREPARE_STEPS.
   */
  String PREPARE_STEPS = "Prepare Steps";

  /**
   * The constant UPGRADE_AUTOSCALING_GROUP.
   */
  String UPGRADE_AUTOSCALING_GROUP = "Upgrade AutoScaling Group";

  String UPGRADE_AUTOSCALING_GROUP_ROUTE = "Switch AutoScaling Group Route";

  String SWAP_AUTOSCALING_GROUP_ROUTE = "Swap Routes";

  String ROLLBACK_AUTOSCALING_GROUP_ROUTE = "Rollback AutoScaling Group Route";
  /**
   * The constant AWS_CODE_DEPLOY.
   */
  String AWS_CODE_DEPLOY = "AWS CodeDeploy";

  /**
   * The constant AWS_LAMBDA.
   */
  String AWS_LAMBDA = "AWS Lambda";

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
   * The constant ROLLBACK_AWS_LAMBDA.
   */
  String ROLLBACK_AWS_LAMBDA = "Rollback AWS Lambda";
  /**
   * The constant ROLLBACK_AWS_AMI_CLUSTER.
   */
  String ROLLBACK_AWS_AMI_CLUSTER = "Rollback AutoScaling Group";
  /**
   * The constant UPGRADE_CONTAINERS.
   */
  String UPGRADE_CONTAINERS = "Upgrade Containers";

  /**
   * The constant HELM_DEPLOY.
   */
  String HELM_DEPLOY = "Helm Deploy";

  /**
   * The constant HELM_ROLLBACK.
   */
  String HELM_ROLLBACK = "Helm Rollback";
  /**
   * The constant ROLLBACK_AWS_CODE_DEPLOY.
   */
  String ROLLBACK_AWS_CODE_DEPLOY = "Rollback AWS CodeDeploy";
  /**
   * The constant ROLLBACK_CONTAINERS.
   */
  String ROLLBACK_CONTAINERS = "Rollback Containers";
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
   * The constant ROLLBACK_PREFIX.
   */
  String ROLLBACK_PREFIX = "Rollback ";
  /**
   * The constant PHASE_NAME_PREFIX.
   */
  String PHASE_NAME_PREFIX = "Phase ";

  /**
   * The constant phaseNamePattern.
   */
  Pattern phaseNamePattern = Pattern.compile("Phase [0-9]+");

  /**
   * The constant WRAP_UP.
   */
  String WRAP_UP = "Wrap Up";

  /**
   * The constant INFRASTRUCTURE_NODE_NAME.
   */
  String INFRASTRUCTURE_NODE_NAME = "Prepare Infra";

  /**
   * The constant PROVISION_NODE_NAME.
   */
  @Deprecated String PROVISION_NODE_NAME = "Provision Nodes";

  /**
   * The constant PROVISION_NODE_NAME.
   */
  String PROVISION_WITH_TERRAFORM_NAME = "Terraform";
  String ROLLBACK_TERRAFORM_NAME = "Terraform Rollback";

  /**
   * The constant PROVISION_NODE_NAME.
   */
  String DE_PROVISION_NODE_NAME = "Deprovision";

  /**
   * The constant SELECT_NODE_NAME.
   */
  String SELECT_NODE_NAME = "Select Nodes";

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
   * The constant WAIT_RESUME_GROUP.
   */
  String WAIT_RESUME_GROUP = "WAIT_RESUME_GROUP";

  /**
   * The constant ONE_TIME_REMINDER.
   */
  String ONE_TIME_REMINDER = "ONE_TIME_REMINDER";

  /**
   * The constant DEFAULT_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_STATE_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000; // 4 hours

  /**
   * The constant DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS = 3 * 60 * 60 * 1000; // 3 hours

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
   * The constant DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days

  /**
   * The constant for Default Env State timeout
   */
  Integer ENV_STATE_TIMEOUT_MILLIS = 12 * 60 * 60 * 1000; // 12 hours

  /**
   * The constant RUNTIME.
   */
  String RUNTIME = "RUNTIME";
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
   * The constant reserved for BuildNO
   */
  String BUILD_NO = "buildNo";

  String TAG = "tag";

  String IMAGE = "image";

  String BUILD_FULL_DISPLAY_NAME = "buildFullDisplayName";

  /**
   * The constant BUCKET_NAME.
   */
  String BUCKET_NAME = "bucketName";

  /**
   * The constant KEY.
   */
  String KEY = "key";

  /**
   * The constant URL.
   */
  String URL = "url";
  /**
   * The constant REVISION.
   */
  String REVISION = "revision";
  /**
   * The constant BUILD_PARAMS.
   */
  String BUILD_PARAMS = "buildParameters";
  /**
   * The constant ARTIFACT_PATH.
   */
  String ARTIFACT_PATH = "artifactPath";
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
   * The constant ARTIFACT_FILE_NAME.
   */
  String ARTIFACT_FILE_NAME = "artifactFileName";
  /**
   * Constant to hold the ARTIFACT_FILE_NAME in the context
   */
  String ARTIFACT_FILE_NAME_VARIABLE = "ARTIFACT_FILE_NAME";

  String ARTIFACT_FILE_SIZE = "artifactFileSize";

  /**
   * The constant DELEGATE_SYNC_CACHE.
   */
  String DELEGATE_SYNC_CACHE = "delegateSyncCache";

  /**
   * A cache to track the trial registration email for rate limiting purpose.
   */
  String TRIAL_EMAIL_CACHE = "trialEmailCache";

  /**
   * The constant USER_CACHE.
   */
  String USER_CACHE = "userCache";

  /**
   * The constant NEW_RELIC_APPLICATION_CACHE.
   */
  String NEW_RELIC_APPLICATION_CACHE = "nrApplicationCache";

  /**
   * The constant USER_PERMISSION_CACHE.
   */
  String USER_PERMISSION_CACHE = "userPermissionCache";

  /**
   * The constant USER_RESTRICTION_CACHE.
   */
  String USER_RESTRICTION_CACHE = "userRestrictionCache";

  /**
   * The constant
   */
  String WHITELIST_CACHE = "whitelistCache";

  /**
   * The constant GIT_USER.
   */
  String GIT_USER = "git";

  String DEPLOYMENT_TYPE_FIELD = "deploymentType";

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
   * The constant DEFAULT_RUNTIME_ENTITY_PAGESIZE.
   */
  int DEFAULT_RUNTIME_ENTITY_PAGESIZE = 20;
  /**
   * The constant DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR.
   */
  String DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR = "20";

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
   * The constant ACTIVITY_ID.
   */
  String ACTIVITY_ID = "activityId";

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
   * The constant SECRET_MASK.
   */
  String SECRET_MASK = "**************";

  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  String DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within PROD environments";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Non-production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within NON_PROD environments";

  /**
   * The cloudwatch metric url.
   */
  String HARNESS_KUBE_CONFIG_PATH = "HARNESS_KUBE_CONFIG_PATH";

  String WORKFLOW_NAME_DATE_FORMAT = "MM/dd/yyyy hh:mm a";
  String INFRA_ROUTE = "infra.route";
  String INFRA_TEMP_ROUTE = "infra.tempRoute";
  String INFRA_ROUTE_PCF = "infra.pcf.route";
  String INFRA_TEMP_ROUTE_PCF = "infra.pcf.tempRoute";

  String PCF_APP_NAME = "pcfAppName";
  String PCF_OLD_APP_NAME = "pcfOldAppName";
  String TOKEN_FIELD = "Bearer Token(HTTP Header)";

  String WINDOWS_HOME_DIR = "%USERPROFILE%";

  String KUBERNETES_SERVICE_SETUP_BLUEGREEN = "Blue/Green Service Setup";
  String ROUTE_UPDATE = "Route Update";
  String KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE = "Swap Primary with Stage";
  String VERIFY_STAGE_SERVICE = "Verify Stage Service";
  String ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM = "ROUTE_UPDATE_ROLLBACK_REQUEST_PARAM";

  // Expression Builder Constants
  String ARTIFACT_SOURCE_USER_NAME_KEY = "username";
  String ARTIFACT_SOURCE_REGISTRY_URL_KEY = "registryUrl";
  String ARTIFACT_SOURCE_REPOSITORY_NAME_KEY = "repositoryName";
  String ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY = "dockerconfig";
  String ARTIFACT_SOURCE_DOCKER_CONFIG_PLACEHOLDER = "${dockerconfig}";

  int YAML_MAX_PARALLEL_COUNT = 20;
  /**
   * Template VAR description
   */
  String ENV_VAR_DESC = "Variable for Environment entity";
  String SERVICE_VAR_DESC = "Variable for Service entity";
  String SERVICE_INFRA_VAR_DESC = "Variable for Service Infra-structure entity";
  String APPD_SERVER_VAR_DESC = "Variable for AppDynamics Server entity";
  String APPD_APP_VAR_DESC = "Variable for AppDynamics Application entity";
  String APPD_TIER_VAR_DESC = "Variable for AppDynamics Tier entity";
  String ELK_SERVER_VAR_DESC = "Variable for Elastic Search Server entity";
  String ELK_INDICES_VAR_DESC = "Variable for Elastic Search Indices entity";
  String CF_AWSCONFIG_VAR_DESC = "Variable for CloudFormation AWS Config entity";
  String HELM_GITCONFIG_VAR_DESC = "Variable for Helm Git Config entity";

  String HARNESS_KUBERNETES_MANAGED_LABEL_KEY = "harness.io/managed";
  String HARNESS_KUBERNETES_APP_LABEL_KEY = "harness.io/application";
  String HARNESS_KUBERNETES_SERVICE_LABEL_KEY = "harness.io/service";
  String HARNESS_KUBERNETES_ENV_LABEL_KEY = "harness.io/environment";
  String HARNESS_KUBERNETES_REVISION_LABEL_KEY = "harness.io/revision";
  String HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY = "harness.io/service-infra-id";

  String URL_STRING = "Url";

  String PRIMARY_SERVICE_NAME_EXPRESSION = "${PRIMARY_SERVICE_NAME}";
  String STAGE_SERVICE_NAME_EXPRESSION = "${STAGE_SERVICE_NAME}";
  String DEPLOYMENT_TRIGGERED_BY = "deploymentTriggeredBy";
  long ARTIFACT_FILE_SIZE_LIMIT = 4L * 1024L * 1024L * 1024L; // 4GB

  String SUPPORT_EMAIL = "support@harness.io";

  /**
   * Trial expires end of day - 14 days from the date of creation.
   */
  int TRIAL_PERIOD = 14;
  /**
   *
   */
  int PAID_PERIOD_IN_YEARS = 1;
  String LICENSE_INFO = "LICENSE_INFO";

  long ML_RECORDS_TTL_MONTHS = 6;

  String HARNESS_IO_KEY_ = "Harness.io";
  String HARNESS_SUPPORT_EMAIL_KEY = "support@harness.io";

  int DEFAULT_TRIAL_LICENSE_UNITS = 100;
  int DEFAULT_COMMUNITY_LICENSE_UNITS = 5;

  int DEFAULT_PAID_LICENSE_UNITS = 100;

  String EMAIL_SUBJECT_ACCOUNT_EXPIRED = "Harness License Expired!";
  String EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE = "Harness License about to Expire!";

  String EMAIL_BODY_ACCOUNT_EXPIRED = "Customer License has Expired";
  String EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE = "Customer License is about to Expire";

  Integer REFERENCED_ENTITIES_TO_SHOW = 10;
  String EMAIL_ID = "EMAIL_ID";
  String ACCOUNT_ID = "ACCOUNT_ID";
  String OLD_LICENSE = "OLD_LICENSE";
  String NEW_LICENSE = "NEW_LICENSE";

  String VALUES_YAML_KEY = "values.yaml";

  // ECS BG
  String BG_VERSION = "BG_VERSION";
  String BG_GREEN = "GREEN";
  String BG_BLUE = "BLUE";
  String ECS_BG_TYPE_DNS = "DNS";
  String ECS_BG_TYPE_ELB = "ELB";

  /**
   * Quartz job detail key names
   */
  String ACCOUNT_ID_KEY = "accountId";
  String APP_ID_KEY = "appId";

  String DEFAULT_USER_GROUP_DESCRIPTION = "Default account admin user group";
  String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  /**
   * Script as Approval
   */
  String SCRIPT_APPROVAL_COMMAND = "Execute Approval Script";
  String SCRIPT_APPROVAL_ENV_VARIABLE = "HARNESS_APPROVAL_STATUS";
  String SCRIPT_APPROVAL_JOB_GROUP = "SHELL_SCRIPT_APPROVAL_JOB";

  String RESOURCE_URI_CREATE_APP = "/api/apps";
  String RESOURCE_URI_CREATE_SERVICE = "/api/services";
  String RESOURCE_URI_CREATE_PROVISIONER = "/api/infrastructure-provisioners";
  String RESOURCE_URI_CREATE_ENVIRONMENT = "/api/environments";
  String RESOURCE_URI_CREATE_WORKFLOW = "/api/workflows";
  String RESOURCE_URI_CREATE_PIPELINE = "/api/pipelines";

  String RESOURCE_URI_CLONE_APP = "/api/apps/[^/]+/clone";
  String RESOURCE_URI_CLONE_SERVICE = "/api/services/[^/]+/clone";
  String RESOURCE_URI_CLONE_PROVISIONER = "/api/infrastructure-provisioners/[^/]+/clone";
  String RESOURCE_URI_CLONE_ENVIRONMENT = "/api/environments/[^/]+/clone";
  String RESOURCE_URI_CLONE_WORKFLOW = "/api/workflows/[^/]+/clone";
  String RESOURCE_URI_CLONE_PIPELINE = "/api/pipelines/[^/]+/clone";

  String RESOURCE_URI_UPDATE_ENVIRONMENT = "/api/environments/[^/]+";
  String RESOURCE_URI_UPDATE_WORKFLOW = "/api/workflows/[^/]+/basic";
  String RESOURCE_URI_UPDATE_PIPELINE = "/api/pipelines/[^/]+";

  String RESOURCE_URI_DELETE_APP = "/api/apps/[^/]+";
  String RESOURCE_URI_DELETE_ENVIRONMENT = "/api/environments/[^/]+";
}
