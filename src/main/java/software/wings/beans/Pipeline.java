/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

/**
 * @author Rishi
 *
 */
@Entity(value = "pipelines", noClassnameStored = true)
public class Pipeline extends Base {
  private String name;
  private String description;
  private List<String> services;
  private String cronSchedule;

  private String stateMachineId;

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

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public Graph getGraph() {
    return Graph;
  }

  public void setGraph(Graph graph) {
    Graph = graph;
  }
}
