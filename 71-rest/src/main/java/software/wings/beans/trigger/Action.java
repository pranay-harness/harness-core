package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = WorkflowAction.class, name = "ORCHESTRATION")
  , @JsonSubTypes.Type(value = PipelineAction.class, name = "PIPELINE")
})
public interface Action {
  enum ActionType { PIPELINE, ORCHESTRATION }
  ActionType getActionType();
}
