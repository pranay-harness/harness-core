package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.Condition.Type;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("PIPELINE_COMPLETION")
@JsonPropertyOrder({"harnessApiVersion"})
public class PipelineCompletionConditionYaml extends ConditionYaml {
  private String pipelineName;

  public PipelineCompletionConditionYaml() {
    super.setType(Type.PIPELINE_COMPLETION.name());
  }
  @Builder
  PipelineCompletionConditionYaml(String pipelineName) {
    super.setType(Type.PIPELINE_COMPLETION.name());
    this.pipelineName = pipelineName;
  }
}
