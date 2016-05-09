/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.sm.StateMachine;

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

  private String applicationId;
  private String stateMachineId;

  @Transient private StateMachine stateMachine;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StateMachine getStateMachine() {
    return stateMachine;
  }

  public void setStateMachine(StateMachine stateMachine) {
    this.stateMachine = stateMachine;
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

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }
}
