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

  public List<PipelineStageElement> getPipelineStageElements() {
    return pipelineStageElements;
  }

  public void setPipelineStageElements(List<PipelineStageElement> pipelineStageElements) {
    this.pipelineStageElements = pipelineStageElements;
  }

  public static class PipelineStageElement {
    private String name;
    private String type;
    private Map<String, Object> properties = new HashMap<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }
  }
}
