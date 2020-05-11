package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.Condition.Type;

@OwnedBy(CDC)
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
