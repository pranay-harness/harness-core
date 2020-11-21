package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactSelectionType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PIPELINE_SOURCE")
@JsonPropertyOrder({"harnessApiVersion"})
public class TriggerArtifactSelectionFromPipelineSourceYaml extends TriggerArtifactSelectionValueYaml {
  @Builder
  public TriggerArtifactSelectionFromPipelineSourceYaml() {
    super.setType(ArtifactSelectionType.PIPELINE_SOURCE.name());
  }
}
