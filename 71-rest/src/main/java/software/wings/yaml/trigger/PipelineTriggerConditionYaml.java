package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("PIPELINE_COMPLETION")
@JsonPropertyOrder({"harnessApiVersion"})
public class PipelineTriggerConditionYaml extends TriggerConditionYaml {
  private String pipelineName;

  @lombok.Builder
  PipelineTriggerConditionYaml(String pipelineName) {
    super.setType("PIPELINE_COMPLETION");
    this.pipelineName = pipelineName;
  }
}
