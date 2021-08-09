package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("PIPELINE")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class PipelineAction implements Action {
  @NotEmpty private String pipelineId;
  @NotEmpty private ActionType actionType = ActionType.PIPELINE;
  private TriggerArgs triggerArgs;
  private transient String pipelineName;
}
