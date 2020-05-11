package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactSelectionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TriggerArtifactSelectionLastDeployed.class, name = "LAST_DEPLOYED")
  , @JsonSubTypes.Type(value = TriggerArtifactSelectionLastCollected.class, name = "LAST_COLLECTED"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionFromSource.class, name = "ARTIFACT_SOURCE"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionFromPipelineSource.class, name = "PIPELINE_SOURCE"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionWebhook.class, name = "WEBHOOK_VARIABLE")
})
public interface TriggerArtifactSelectionValue {
  enum ArtifactSelectionType { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  ArtifactSelectionType getArtifactSelectionType();
}
