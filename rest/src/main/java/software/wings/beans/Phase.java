package software.wings.beans;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Phase.
 */
@Entity(value = "phases", noClassnameStored = true)
public class Phase {
  @Id private ObjectId id;

  private String name;
  private String description;
  private String compName;
  private String envName;
  private Map<String, List<String>> hostInstances = new HashMap<>();

  private List<TemplateExpression> templateExpressions;

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
   * Gets comp name.
   *
   * @return the comp name
   */
  public String getCompName() {
    return compName;
  }

  /**
   * Sets comp name.
   *
   * @param compName the comp name
   */
  public void setCompName(String compName) {
    this.compName = compName;
  }

  /**
   * Gets host instances.
   *
   * @return the host instances
   */
  public Map<String, List<String>> getHostInstances() {
    return hostInstances;
  }

  /**
   * Sets host instances.
   *
   * @param hostInstances the host instances
   */
  public void setHostInstances(Map<String, List<String>> hostInstances) {
    this.hostInstances = hostInstances;
  }

  /**
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }

  /**
   * Get template expressions
   * @return
   */
  public List<TemplateExpression> getTemplateExpressions() {
    return templateExpressions;
  }

  /**
   * Set template expressions
   * @param templateExpressions
   */
  public void setTemplateExpressions(List<TemplateExpression> templateExpressions) {
    this.templateExpressions = templateExpressions;
  }
}
