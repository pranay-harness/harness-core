package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = ArtifactCondition.class, name = "NEW_ARTIFACT") })
public interface Condition {
  enum Type { NEW_ARTIFACT, PIPELINE_COMPLETION, SCHEDULED, WEBHOOK, NEW_INSTANCE }
}