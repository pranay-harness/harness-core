package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "actionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = WorkflowAction.class, name = "WORKFLOW")
  , @JsonSubTypes.Type(value = PipelineAction.class, name = "PIPELINE")
})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public interface Action {
  enum ActionType { PIPELINE, WORKFLOW }
  ActionType getActionType();
}
