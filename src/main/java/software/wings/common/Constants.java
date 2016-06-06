package software.wings.common;

/**
 * Common constants across application.
 *
 * @author Rishi
 */
public interface Constants {
  String CATALOG_STENCILS = "STENCILS";
  String DEFAULT_WORKFLOW_NAME = "MAIN";
  char WILD_CHAR = '*';

  String EXPRESSION_LIST_SUFFIX = ".list()";
  String EXPRESSION_NAME_SUFFIX = ".names()";

  String STATIC_CATALOG_URL = "/configs/catalogs.yml";
  String SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL = "/configs/simple_workflow_default_graph.json";
  public static final String SIMPLE_ORCHESTRATION_PARAMS = "SIMPLE_ORCHESTRATION_PARAMS";
}
