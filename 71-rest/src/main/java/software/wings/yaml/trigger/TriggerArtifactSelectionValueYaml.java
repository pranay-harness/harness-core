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
  @Type(value = TriggerArtifactSelectionFromSourceYaml.class, name = "ARTIFACT_SOURCE")
  , @Type(value = TriggerArtifactSelectionFromPipelineSourceYaml.class, name = "PIPELINE_SOURCE"),
      @Type(value = TriggerArtifactSelectionLastCollectedYaml.class, name = "LAST_COLLECTED"),
      @Type(value = TriggerArtifactSelectionLastDeployedYaml.class, name = "LAST_DEPLOYED"),
      @Type(value = TriggerArtifactSelectionWebhookYaml.class, name = "WEBHOOK_VARIABLE")
})
public class TriggerArtifactSelectionValueYaml extends BaseYamlWithType {
  public TriggerArtifactSelectionValueYaml(String type) {
    super(type);
  }
}
