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
   * The constant SIMPLE_ORCHESTRATION_NAME.
   */
  String SIMPLE_ORCHESTRATION_NAME = "DefaultSimpleWorkflow";
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

  String PROD_ENV = "Production";
  String UAT_ENV = "User Acceptance";
  String QA_ENV = "Quality Assurance";
  String DEV_ENV = "Development";
}
