package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = WorkflowAction.class, name = "WORKFLOW")
  , @JsonSubTypes.Type(value = PipelineAction.class, name = "PIPELINE")
})
public interface Action {
  enum ActionType { PIPELINE, WORKFLOW }
  ActionType getActionType();
}
