/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

/**
 * @author Rishi
 */
@Entity(value = "pipelines", noClassnameStored = true)
public class Pipeline extends Workflow {
  private List<String> services;
  private String cronSchedule;

  @Transient private Graph Graph;

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }

  @Override
  public Graph getGraph() {
    return Graph;
  }

  @Override
  public void setGraph(Graph graph) {
    Graph = graph;
  }
}
