package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PipelineYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public List<PipelineStageYaml> pipelineStages = new ArrayList<>();

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

  public List<PipelineStageYaml> getPipelineStages() {
    return pipelineStages;
  }

  public void setPipelineStages(List<PipelineStageYaml> pipelineStages) {
    this.pipelineStages = pipelineStages;
  }
}