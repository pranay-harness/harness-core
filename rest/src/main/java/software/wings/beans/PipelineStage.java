package software.wings.beans;

import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/17/16.
 */
public class PipelineStage {
  private String name;
  private boolean parallel;
  private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
  private boolean valid = true;
  private String validationMessage;
  /**
   * Instantiates a new Pipeline stage.
   */
  public PipelineStage() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isParallel() {
    return parallel;
  }

  public void setParallel(boolean parallel) {
    this.parallel = parallel;
  }

  /**
   * Instantiates a new Pipeline stage.
   *
   * @param pipelineStageElements the pipeline stage elements
   */
  public PipelineStage(List<PipelineStageElement> pipelineStageElements) {
    this.pipelineStageElements = pipelineStageElements;
  }

  /**
   * Gets pipeline stage elements.
   *
   * @return the pipeline stage elements
   */
  public List<PipelineStageElement> getPipelineStageElements() {
    return pipelineStageElements;
  }

  /**
   * Sets pipeline stage elements.
   *
   * @param pipelineStageElements the pipeline stage elements
   */
  public void setPipelineStageElements(List<PipelineStageElement> pipelineStageElements) {
    this.pipelineStageElements = pipelineStageElements;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  /**
   * The type Pipeline stage element.
   */
  public static class PipelineStageElement {
    private String name;
    private String type;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> workflowVariables = new HashMap<>();

    private boolean valid = true;
    private String validationMessage;

    /**
     * Instantiates a new Pipeline stage element.
     */
    public PipelineStageElement() {}

    /**
     * Instantiates a new Pipeline stage element.
     *
     * @param name       the name
     * @param type       the type
     * @param properties the properties
     */
    public PipelineStageElement(String name, String type, Map<String, Object> properties) {
      this.name = name;
      this.type = type;
      this.properties = properties;
    }

    /**
     * Instantiates a new Pipeline stage element.
     *
     * @param name       the name
     * @param type       the type
     * @param properties the properties
     */
    public PipelineStageElement(
        String name, String type, Map<String, Object> properties, Map<String, String> workflowVariables) {
      this.name = name;
      this.type = type;
      this.properties = properties;
      this.workflowVariables = workflowVariables;
    }
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
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
      return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(String type) {
      this.type = type;
    }

    /**
     * Gets properties.
     *
     * @return the properties
     */
    public Map<String, Object> getProperties() {
      return properties;
    }

    /**
     * Sets properties.
     *
     * @param properties the properties
     */
    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }

    /**
     * Get workflow variables
     *
     * @return
     */
    public Map<String, String> getWorkflowVariables() {
      return workflowVariables;
    }

    /**
     * Set workflow variables
     *
     * @param workflowVariables
     */
    public void setWorkflowVariables(Map<String, String> workflowVariables) {
      this.workflowVariables = workflowVariables;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public String getValidationMessage() {
      return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private boolean parallel;
    private String workflowName;
    private List<NameValuePair.Yaml> workflowVariables = Lists.newArrayList();

    @Builder
    public Yaml(
        String type, String name, boolean parallel, String workflowName, List<NameValuePair.Yaml> workflowVariables) {
      super(type);
      this.name = name;
      this.parallel = parallel;
      this.workflowName = workflowName;
      this.workflowVariables = workflowVariables;
    }
  }
}
