/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;

import javax.validation.constraints.NotNull;

/**
 * @author Rishi
 *
 */
public class Workflow extends Base {
  @NotNull private String name;
  private String description;

  @Transient private Graph graph;

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
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }
}
