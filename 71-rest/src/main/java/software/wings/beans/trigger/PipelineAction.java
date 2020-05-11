package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("PIPELINE")
public class PipelineAction implements Action {
  @NotEmpty private String pipelineId;
  @NotEmpty private ActionType actionType = ActionType.PIPELINE;
  private TriggerArgs triggerArgs;
  private transient String pipelineName;
}
