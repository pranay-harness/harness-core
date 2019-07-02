package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

@Value
@Builder
@JsonTypeName("WorkflowAction")
public class WorkflowAction implements Action {
  @NotEmpty private String workflowId;
  @NotEmpty private ActionType actionType = ActionType.ORCHESTRATION;
  private TriggerArgs triggerArgs;
  @Transient private String workflowName;
}
