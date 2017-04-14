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

  String INSTANCE_LIST_PARAMS = "INSTANCE_LIST_PARAMS";

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

  String ALL = "All";
  /**
   * The constant to represent all environment
   */
  String ALL_ENV = "__ALL_ENVIRONMENTS__";

  String WINGS_VARIABLE_PREFIX = "${";
  String WINGS_VARIABLE_SUFFIX = "}";

  String PRE_DEPLOYMENT = "Pre-Deployment";
  String POST_DEPLOYMENT = "Post-Deployment";
  String DISABLE_SERVICE = "Disable Service";
  String ENABLE_SERVICE = "Enable Service";
  String DEPLOY_SERVICE = "Deploy Service";
  String STOP_SERVICE = "Stop Service";
  String VERIFY_SERVICE = "Verify Service";

  String DEPLOY_CONTAINERS = "Deploy Containers";
  String SETUP_CONTAINER = "Setup Container";
  String SETUP_CLUSTER = "Setup Cluster";

  String ECS_SERVICE_SETUP = "ECS Service Setup";
  String UPGRADE_CONTAINERS = "Upgrade Containers";

  String SUB_WORKFLOW_ID = "subWorkflowId";
  String SUB_WORKFLOW = "SUB_WORKFLOW";
  String ROLLBACK_PREFIX = "Rollback ";
  String PHASE_NAME_PREFIX = "Phase ";

  String WRAP_UP = "Wrap Up";
  String PROVISION_NODE_NAME = "Provision Nodes";
  String DE_PROVISION_NODE = "De-Provision Nodes";
  String STEP_VALIDATION_MESSAGE = "Some fields %s are found to be invalid/incomplete.";
  String PHASE_STEP_VALIDATION_MESSAGE = "Some steps %s are found to be invalid/incomplete.";
  String PHASE_VALIDATION_MESSAGE = "Some steps %s are found to be invalid/incomplete.";
  String WORKFLOW_VALIDATION_MESSAGE = "Some phases %s are found to be invalid/incomplete.";

  String WAIT_RESUME_GROUP = "WAIT_RESUME_GROUP";

  Integer DEFAULT_STATE_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30 minutes

  Integer DEFAULT_PARENT_STATE_TIMEOUT_MILLIS = 60 * 60 * 1000; // 60 minutes
}
