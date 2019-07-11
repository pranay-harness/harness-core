package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TriggerArtifactSelectionWorkflow.class, name = "ORCHESTRATION")
  , @JsonSubTypes.Type(value = TriggerArtifactSelectionPipeline.class, name = "PIPELINE"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionArtifact.class, name = "ARTIFACT")
})
public interface TriggerArtifactSelectionValue {
  enum ArtifactVariableType { PIPELINE, ORCHESTRATION, ARTIFACT }

  ArtifactVariableType getArtifactVariableType();
}
