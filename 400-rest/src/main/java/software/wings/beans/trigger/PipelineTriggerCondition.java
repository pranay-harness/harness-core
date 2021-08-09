package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.trigger.TriggerConditionType.PIPELINE_COMPLETION;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@JsonTypeName("PIPELINE_COMPLETION")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PipelineTriggerCondition extends TriggerCondition {
  @NotEmpty private String pipelineId;
  private String pipelineName;

  public PipelineTriggerCondition() {
    super(PIPELINE_COMPLETION);
  }

  public PipelineTriggerCondition(String pipelineId, String pipelineName) {
    this();
    this.pipelineId = pipelineId;
    this.pipelineName = pipelineName;
  }
}
