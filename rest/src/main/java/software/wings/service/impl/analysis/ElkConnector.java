package software.wings.service.impl.analysis;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

  @SuppressFBWarnings("ME_ENUM_FIELD_SETTER")
  public void setName(String name) {
    this.name = name;
  }
}
