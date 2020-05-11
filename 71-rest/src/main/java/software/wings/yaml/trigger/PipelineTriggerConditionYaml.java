package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("PIPELINE_COMPLETION")
@JsonPropertyOrder({"harnessApiVersion"})
public class PipelineTriggerConditionYaml extends TriggerConditionYaml {
  private String pipelineName;

  public PipelineTriggerConditionYaml() {
    super.setType("PIPELINE_COMPLETION");
  }
  @lombok.Builder
  PipelineTriggerConditionYaml(String pipelineName) {
    super.setType("PIPELINE_COMPLETION");
    this.pipelineName = pipelineName;
  }
}
