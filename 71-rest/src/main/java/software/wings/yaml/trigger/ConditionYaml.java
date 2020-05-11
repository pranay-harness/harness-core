package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = NewArtifactConditionYaml.class, name = "NEW_ARTIFACT")
  , @Type(value = ScheduledConditionYaml.class, name = "SCHEDULED"),
      @Type(value = PipelineCompletionConditionYaml.class, name = "PIPELINE_COMPLETION"),
      @Type(value = WebhookConditionYaml.class, name = "WEBHOOK")
})
public class ConditionYaml extends BaseYamlWithType {
  public ConditionYaml(String type) {
    super(type);
  }
}
