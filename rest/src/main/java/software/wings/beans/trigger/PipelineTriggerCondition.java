package software.wings.beans.trigger;

import static software.wings.beans.trigger.TriggerConditionType.PIPELINE_COMPLETION;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@JsonTypeName("PIPELINE_COMPLETION")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PipelineTriggerCondition extends TriggerCondition {
  @NotEmpty String pipelineId;

  public PipelineTriggerCondition() {
    super(PIPELINE_COMPLETION);
  }

  public PipelineTriggerCondition(String pipelineId) {
    this();
    this.pipelineId = pipelineId;
  }
}
