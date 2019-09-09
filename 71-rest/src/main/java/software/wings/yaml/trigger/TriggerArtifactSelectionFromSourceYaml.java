package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactSelectionType;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("ARTIFACT_SOURCE")
@JsonPropertyOrder({"harnessApiVersion"})
public class TriggerArtifactSelectionFromSourceYaml extends TriggerArtifactSelectionValueYaml {
  @Builder
  public TriggerArtifactSelectionFromSourceYaml() {
    super.setType(ArtifactSelectionType.ARTIFACT_SOURCE.name());
  }
}
