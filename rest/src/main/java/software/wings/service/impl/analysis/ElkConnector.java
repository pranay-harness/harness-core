package software.wings.service.impl.analysis;

/**
 * Created by sriram_parthasarathy on 10/5/17.
 */
public enum ElkConnector {
  ELASTIC_SEARCH_SERVER("Elastic search server"),
  KIBANA_SERVER("Kibana Server");

  private String name;

  ElkConnector(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
