/**
 *
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * The Class Workflow.
 *
 * @author Rishi
 */
public class Workflow extends Base {
  @NotNull private String name;
  private String description;

  @Transient private List<Service> services = new ArrayList<>();

  private ErrorStrategy errorStrategy;

  private Integer defaultVersion;

  private Graph graph;

  @JsonIgnore @Transient private boolean setAsDefault;

  @JsonIgnore @Transient private String notes;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public List<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<Service> services) {
    this.services = services;
  }

  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  public Integer getDefaultVersion() {
    return defaultVersion;
  }

  public void setDefaultVersion(Integer defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Getter for property 'setAsDefault'.
   *
   * @return Value for property 'setAsDefault'.
   */
  @JsonIgnore
  public boolean getSetAsDefault() {
    return setAsDefault;
  }

  /**
   * Setter for property 'setAsDefault'.
   *
   * @param setAsDefault Value to set for property 'setAsDefault'.
   */
  @JsonProperty
  public void setSetAsDefault(boolean setAsDefault) {
    this.setAsDefault = setAsDefault;
  }

  /**
   * Getter for property 'notes'.
   *
   * @return Value for property 'notes'.
   */
  @JsonIgnore
  public String getNotes() {
    return notes;
  }

  /**
   * Setter for property 'notes'.
   *
   * @param notes Value to set for property 'notes'.
   */
  @JsonProperty
  public void setNotes(String notes) {
    this.notes = notes;
  }
}
