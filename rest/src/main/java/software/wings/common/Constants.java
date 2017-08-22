package software.wings.common;

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
   * The constant CONTAINER_UPGRADE_REQUEST_PARAM.
   */
  String CONTAINER_UPGRADE_REQUEST_PARAM = "CONTAINER_UPGRADE_REQUEST_PARAM";

  /**
   * The constant AWS_CODE_DEPLOY_REQUEST_PARAM.
   */
  String AWS_CODE_DEPLOY_REQUEST_PARAM = "AWS_CODE_DEPLOY_REQUEST_PARAM";

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
  /**
   * The constant PREPARE_STEPS.
   */
  String PREPARE_STEPS = "Prepare Steps";
  /**
   * The constant AWS_CODE_DEPLOY.
   */
  String AWS_CODE_DEPLOY = "AWS CodeDeploy";
  /**
   * The constant UPGRADE_CONTAINERS.
   */
  String UPGRADE_CONTAINERS = "Upgrade Containers";
  /**
   * The constant ROLLBACK_AWS_CODE_DEPLOY.
   */
  String ROLLBACK_AWS_CODE_DEPLOY = "Rollback AWS CodeDeploy";
  /**
   * The constant ROLLBACK_CONTAINERS.
   */
  String ROLLBACK_CONTAINERS = "Rollback Containers";
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
   * The constant WRAP_UP.
   */
  String WRAP_UP = "Wrap Up";
  /**
   * The constant PROVISION_NODE_NAME.
   */
  String PROVISION_NODE_NAME = "Provision Nodes";
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
   * The constant WAIT_RESUME_GROUP.
   */
  String WAIT_RESUME_GROUP = "WAIT_RESUME_GROUP";

  /**
   * The constant DEFAULT_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_STATE_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30 minutes

  /**
   * O
   * The constant DEFAULT_PARENT_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_PARENT_STATE_TIMEOUT_MILLIS = 60 * 60 * 1000; // 60 minutes

  /**
   * The constant DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS.
   */
  Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days

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
   * The constant DEFAULT_SYNC_CALL_TIMEOUT.
   */
  int DEFAULT_SYNC_CALL_TIMEOUT = 25 * 1000; // 25 seconds
  /**
   * The constant DEFAULT_ASYNC_CALL_TIMEOUT.
   */
  int DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000; // 10 minutes

  /**
   * The constant BUILD_NO.
   */
  String BUILD_NO = "buildNo";
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
   * Template expression constants
   */
  String ENTITY_TYPE = "entityType";
  String ARTIFACT_TYPE = "artifactType";
  String RELATED_FIELD = "relatedField";

  /**
   * The constant S3.
   */
  String S3 = "S3";

  /**
   * The constant FILE_CONTENT_NOT_STORED.
   */
  String FILE_CONTENT_NOT_STORED = "__FILE_CONTENT_NOT_STORED__";
}
