package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

@Value
@Builder
@JsonTypeName("PIPELINE")
public class PipelineAction implements Action {
  @NotEmpty private String pipelineId;
  @NotEmpty private ActionType actionType = ActionType.PIPELINE;
  private TriggerArgs triggerArgs;
  @Transient private String pipelineName;
}
