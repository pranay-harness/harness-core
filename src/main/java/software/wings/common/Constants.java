package software.wings.common;

/**
 * Common constants across application.
 *
 * @author Rishi
 */
public interface Constants {
  static final String CATALOG_STENCILS = "STENCILS";
  static final String DEFAULT_WORKFLOW_NAME = "MAIN";
  static final char WILD_CHAR = '*';

  static final String EXPRESSION_LIST_SUFFIX = ".list()";
  static final String EXPRESSION_NAME_SUFFIX = ".names()";

  String STATIC_CATALOG_URL = "/configs/catalogs.yml";
  String SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL = "/configs/simple_workflow_default_graph.json";
  static final String SERVICE_INSTANCE_IDS_PARAMS = "SERVICE_INSTANCE_IDS_PARAMS";
  static final String SIMPLE_ORCHESTRATION_NAME = "DefaultSimpleWorkflow";
  static final String SIMPLE_ORCHESTRATION_DESC = "This is a simple workflow designed to trigger multiple instances";
  static final String SIMPLE_WORKFLOW_REPEAT_STRATEGY = "SIMPLE_WORKFLOW_REPEAT_STRATEGY";
}
