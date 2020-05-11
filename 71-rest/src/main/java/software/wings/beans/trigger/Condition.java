package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactCondition.class, name = "NEW_ARTIFACT")
  , @JsonSubTypes.Type(value = ScheduledCondition.class, name = "SCHEDULED"),
      @JsonSubTypes.Type(value = PipelineCondition.class, name = "PIPELINE_COMPLETION"),
      @JsonSubTypes.Type(value = WebhookCondition.class, name = "WEBHOOK")
})
public interface Condition {
  enum Type { NEW_ARTIFACT, PIPELINE_COMPLETION, SCHEDULED, WEBHOOK, NEW_INSTANCE }
  Type getType();
}
