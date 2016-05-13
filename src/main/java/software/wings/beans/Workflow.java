/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;

/**
 * @author Rishi
 *
 */
public class Workflow extends Base {
  private String name;
  private String description;

  @Transient private Graph Graph;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Graph getGraph() {
    return Graph;
  }

  public void setGraph(Graph graph) {
    Graph = graph;
  }
}
