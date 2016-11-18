package software.wings.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/17/16.
 */
public class PipelineStage {
  private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();

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

  /**
   * The type Pipeline stage element.
   */
  public static class PipelineStageElement {
    private String name;
    private String type;
    private Map<String, Object> properties = new HashMap<>();

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
  }
}
