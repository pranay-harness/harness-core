package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;

import java.util.List;

public class PipelineYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public List<PipelineStage> pipelineStages;

  public PipelineYaml() {}

  public PipelineYaml(Pipeline pipeline) {
    this.name = pipeline.getName();
    this.description = pipeline.getDescription();
    this.pipelineStages = pipeline.getPipelineStages();
  }

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

  public List<PipelineStage> getPipelineStages() {
    return pipelineStages;
  }

  public void setPipelineStages(List<PipelineStage> pipelineStages) {
    this.pipelineStages = pipelineStages;
  }
}