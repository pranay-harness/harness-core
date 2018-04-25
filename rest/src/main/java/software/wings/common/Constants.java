package software.wings.common;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Common constants across application.
 *
 * @author Rishi
 */
public interface Constants {
  /**
   * The constant CATALOG_STENCILS.
   */
  String CATALOG_STENCILS = "STENCILS";
  /**
   * The constant DEFAULT_WORKFLOW_NAME.
   */
  String DEFAULT_WORKFLOW_NAME = "MAIN";
  /**
   * The constant WILD_CHAR.
   */
  char WILD_CHAR = '*';

  /**
   * The constant EXPRESSION_LIST_SUFFIX.
   */
  String EXPRESSION_LIST_SUFFIX = ".list()";

  /**
   * The constant EXPRESSION_PARTITIONS_SUFFIX.
   */
  String EXPRESSION_PARTITIONS_SUFFIX = ".partitions()";
  /**
   * The constant EXPRESSION_NAME_SUFFIX.
   */
  String EXPRESSION_NAME_SUFFIX = ".names()";

  /**
   * The constant STATIC_CATALOG_URL.
   */
  String STATIC_CATALOG_URL = "/configs/catalogs.yml";

  /**
   * The constant NOTIFICATION_TEMPLATE_PATH.
   */
  String NOTIFICATION_TEMPLATE_PATH = "/notificationtemplates/notification_templates.yml";
  /**
   * The constant SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL.
   */
  String SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL = "/configs/simple_workflow_default_graph.json";
  /**
   * The constant SERVICE_INSTANCE_IDS_PARAMS.
   */
  String SERVICE_INSTANCE_IDS_PARAMS = "SERVICE_INSTANCE_IDS_PARAMS";

  /**
   * The constant SERVICE_INSTANCE_ARTIFACT_PARAMS.
   */
  String SERVICE_INSTANCE_ARTIFACT_PARAMS = "SERVICE_INSTANCE_ARTIFACT_PARAMS";

  /**
   * The constant CONTAINER_ROLLBACK_REQUEST_PARAM.
   */
  String CONTAINER_ROLLBACK_REQUEST_PARAM = "CONTAINER_ROLLBACK_REQUEST_PARAM";

  /**
   * The constant AWS_CODE_DEPLOY_REQUEST_PARAM.
   */
  String AWS_CODE_DEPLOY_REQUEST_PARAM = "AWS_CODE_DEPLOY_REQUEST_PARAM";

  /**
   * The constant AWS_LAMBDA_REQUEST_PARAM.
   */
  String AWS_LAMBDA_REQUEST_PARAM = "AWS_LAMBDA_REQUEST_PARAM";

  /**
   * The constant AWS_LAMBDA_REQUEST_PARAM.
   */
  String AWS_LAMBDA_FUNCTION_PARAM = "AWS_LAMBDA_FUNCTION_PARAM";

  /**
   * The constant INSTANCE_LIST_PARAMS.
   */
  String INSTANCE_LIST_PARAMS = "INSTANCE_LIST_PARAMS";
  /**
   * The constant PHASE_PARAM.
   */
  String PHASE_PARAM = "PHASE_PARAM";
  /**
   * The constant SIMPLE_ORCHESTRATION_NAME.
   */
  String SIMPLE_ORCHESTRATION_NAME = "Default Adhoc Workflow";
  /**
   * The constant SIMPLE_ORCHESTRATION_DESC.
   */
  String SIMPLE_ORCHESTRATION_DESC = "This is a simple workflow designed to trigger multiple instances";
  /**
   * The constant SIMPLE_WORKFLOW_REPEAT_STRATEGY.
   */
  String SIMPLE_WORKFLOW_REPEAT_STRATEGY = "SIMPLE_WORKFLOW_REPEAT_STRATEGY";

  /**
   * The constant SIMPLE_WORKFLOW_COMMAND_NAME.
   */
  String SIMPLE_WORKFLOW_COMMAND_NAME = "SIMPLE_WORKFLOW_COMMAND_NAME";
  /**
   * The constant SUMMARY_PAYLOAD_LIMIT.
   */
  int SUMMARY_PAYLOAD_LIMIT = 1024;

  /**
   * The constant PROD_ENV.
   */
  String PROD_ENV = "Production";
  /**
   * The constant UAT_ENV.
   */
  String UAT_ENV = "User Acceptance";
  /**
   * The constant QA_ENV.
   */
  String QA_ENV = "Quality Assurance";
  /**
   * The constant DEV_ENV.
   */
  String DEV_ENV = "Development";

  /**
   * The constant ALL.
   */
  String ALL = "All";
  /**
   * The constant to represent all environment
   */
  String ALL_ENV = "__ALL_ENVIRONMENTS__";

  /**
   * The constant WINGS_VARIABLE_PREFIX.
   */
  String WINGS_VARIABLE_PREFIX = "${";
  /**
   * The constant WINGS_VARIABLE_SUFFIX.
   */
  String WINGS_VARIABLE_SUFFIX = "}";

  String ROLLING_PHASE_PREFIX = "Rolling Phase ";

  /**
   * The constant PRE_DEPLOYMENT.
   */
  String PRE_DEPLOYMENT = "Pre-Deployment";
  /**
   * The constant POST_DEPLOYMENT.
   */
  String POST_DEPLOYMENT = "Post-Deployment";
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

  /**
   * The constant DEPLOY_CONTAINERS.
   */
  String DEPLOY_CONTAINERS = "Deploy Containers";
  /**
   * The constant SETUP_CONTAINER.
   */
  String SETUP_CONTAINER = "Setup Container";
  /**
   * The constant SETUP_CLUSTER.
   */
  String SETUP_CLUSTER = "Setup Cluster";
  /**
   * The constant ECS_SERVICE_SETUP.
   */
  String ECS_SERVICE_SETUP = "ECS Service Setup";

  String SETUP = "Setup";

  String DEPLOY = "Deploy";

  String PCF_SETUP = "PCF Service Setup";

  String PCF_RESIZE = "PCF Resize Service";

  String ROLLBACK = "Rollback";

  String PCF_ROLLBACK = "PCF Service Rollback";

  String PCF_ROUTE_SWAP = "Switch Route";
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
   * The constant SETUP_AUTOSCALING_GROUP.
   */
  String SETUP_AUTOSCALING_GROUP = "Setup AutoScaling Group";

  /**
   * The constant UPGRADE_AUTOSCALING_GROUP.
   */
  String UPGRADE_AUTOSCALING_GROUP = "Upgrade AutoScaling Group";
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
   * The constant PROVISION_NODE_NAME.
   */
  String PROVISION_NODE_NAME = "Provision Nodes";

  /**
   * The constant SELECT_NODE_NAME.
   */
  String SELECT_NODE_NAME = "Select Nodes";

  /**
   * The constant DE_PROVISION_NODE.
   */
  String DE_PROVISION_NODE = "De-Provision Nodes";
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
  Integer DEFAULT_STATE_TIMEOUT_MILLIS = 60 * 60 * 1000; // 60 minutes

  /**
   * The constant DEFAULT_STATE_TIMEOUT_MILLIS.
   */
  long DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes

  /**
   * O
   * The constant DEFAULT_PARENT_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_PARENT_STATE_TIMEOUT_MILLIS = 90 * 60 * 1000; // 90 minutes

  /**
   * The constant DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days

  /**
   * The constant for Default Env State timeout
   */
  Integer ENV_STATE_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000; // 4 hours

  /**
   * The constant RUNTIME.
   */
  String RUNTIME = "RUNTIME";
  /**
   * The constant NOT_REGISTERED.
   */
  String NOT_REGISTERED = "<Not registered yet>";
  /**
   * The constant MAINTENANCE.
   */
  String MAINTENANCE = "maintenance";

  /**
   * The constant DELEGATE_DIR.
   */
  String DELEGATE_DIR = "harness-delegate";

  /**
   * The constant DELEGATE_NAME.
   */
  String DELEGATE_NAME = "Harness delegate";

  /**
   * The constant MAX_DELEGATE_LAST_HEARTBEAT.
   */
  long MAX_DELEGATE_LAST_HEARTBEAT = (3 * 60 * 1000) + (15 * 1000); // 3 minutes 15 seconds

  /**
   * The constant DEFAULT_SYNC_CALL_TIMEOUT.
   */
  long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000; // 1 minute

  /**
   * The constant DEFAULT_ASYNC_CALL_TIMEOUT.
   */
  long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000; // 10 minutes

  /**
   * The constant DEFAULT_STEADY_STATE_TIMEOUT.
   */
  int DEFAULT_STEADY_STATE_TIMEOUT = 10;

  /**
   * The constant BUILD_NO.
   */
  String BUILD_NO = "buildNo";

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
   * The constant ARTIFACT_FILE_NAME.
   */
  String ARTIFACT_FILE_NAME = "artifactFileName";
  /**
   * Constant to hold the ARTIFACT_FILE_NAME in the context
   */
  String ARTIFACT_FILE_NAME_VARIABLE = "ARTIFACT_FILE_NAME";

  /**
   * The constant DELEGATE_SYNC_CACHE.
   */
  String DELEGATE_SYNC_CACHE = "delegateSyncCache";
  /**
   * The constant USER_CACHE.
   */
  String USER_CACHE = "userCache";

  /**
   * The constant USER_PERMISSION_CACHE.
   */
  String USER_PERMISSION_CACHE = "userPermissionCache";

  /**
   * The constant
   */
  String WHITELIST_CACHE = "whitelistCache";

  /**
   * Template expression constants
   */
  String ENTITY_TYPE = "entityType";
  /**
   * The constant ARTIFACT_TYPE.
   */
  String ARTIFACT_TYPE = "artifactType";
  /**
   * The constant RELATED_FIELD.
   */
  String RELATED_FIELD = "relatedField";
  /**
   * The constant STATE_TYPE.
   */
  String STATE_TYPE = "stateType";

  /**
   * The constant GIT_USER.
   */
  String GIT_USER = "git";

  /**
   * The constant STATE_TYPE.
   */
  String PARENT_FIELDS = "parentFields";

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
   * The constant HTTP_RESPONSE_CODE.
   */
  String HTTP_RESPONSE_CODE = "httpResponseCode";
  /**
   * The constant HTTP_RESPONSE_BODY.
   */
  String HTTP_RESPONSE_BODY = "httpResponseBody";
  /**
   * The constant ASSERTION_STATEMENT.
   */
  String ASSERTION_STATEMENT = "assertionStatement";
  /**
   * The constant ASSERTION_STATUS.
   */
  String ASSERTION_STATUS = "assertionStatus";
  /**
   * The constant XPATH.
   */
  String XPATH = "xpath('//status/text()')";
  /**
   * The constant JSONPATH.
   */
  String JSONPATH = "jsonpath('health.status')";

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
   * The constant DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT.
   */
  int DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT = 10;

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

  /**
   * The constant HARNESS_APP.
   */
  String HARNESS_APP = "harness-app";
  /**
   * The constant HARNESS_SERVICE.
   */
  String HARNESS_SERVICE = "harness-service";
  /**
   * The constant HARNESS_ENV.
   */
  String HARNESS_ENV = "harness-env";
  /**
   * The constant HARNESS_REVISION.
   */
  String HARNESS_REVISION = "harness-revision";

  /**
   * The constant DEFAULT_AWS_HOST_NAME_CONVENTION.
   */
  String DEFAULT_AWS_HOST_NAME_CONVENTION = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";

  /**
   * The constant APP_ID.
   */
  String APP_ID = "appId";
  /**
   * The constant UUID.
   */
  String UUID = ID_KEY;

  /**
   * The constant SECRET_MASK.
   */
  String SECRET_MASK = "**************";

  /**
   * The constant CORRELATION_ID.
   */
  String CORRELATION_ID = "correlationId";

  /**
   * The Auto downloaded.
   */
  List<String> autoDownloaded = asList(DOCKER.name(), ECR.name(), GCR.name(), ACR.name(), AMAZON_S3.name(), AMI.name());
  /**
   * The constant DEPLOYMENT.
   */
  String DEPLOYMENT = "DEPLOYMENT";
  /**
   * The constant DEFAULT_OLD_USER_GROUP_NAME.
   */
  String DEFAULT_OLD_USER_GROUP_NAME = "ADMIN";
  /**
   * The constant DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME.
   */
  String DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME = "Account Administrator";
  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_NAME.
   */
  String DEFAULT_PROD_SUPPORT_USER_GROUP_NAME = "Production Support";
  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  String DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within PROD environments";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME.
   */
  String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME = "Non-Production Support";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Non-production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within NON_PROD environments";

  /**
   * The constant KUBERNETES_KUBECONFIG_PLACEHOLDER.
   */
  String KUBERNETES_KUBECONFIG_PLACEHOLDER = "Kubernetes-Kubeconfig-Placeholder";
}
