package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class PipelineStageYaml extends GenericYaml {
  @YamlSerialize public List<PipelineStageElementYaml> pipelineStageElements = new ArrayList<>();

  public List<PipelineStageElementYaml> getPipelineStageElements() {
    return pipelineStageElements;
  }

  public void setPipelineStageElements(List<PipelineStageElementYaml> pipelineStageElements) {
    this.pipelineStageElements = pipelineStageElements;
  }
}
