package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "conditionType", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactTriggerCondition.class, name = "NEW_ARTIFACT")
  , @JsonSubTypes.Type(value = PipelineTriggerCondition.class, name = "PIPELINE_COMPLETION"),
      @JsonSubTypes.Type(value = ScheduledTriggerCondition.class, name = "SCHEDULED"),
      @JsonSubTypes.Type(value = WebHookTriggerCondition.class, name = "WEBHOOK")
})
@Data
public abstract class TriggerCondition {
  @NotNull private TriggerConditionType conditionType;

  public TriggerCondition(TriggerConditionType conditionType) {
    this.conditionType = conditionType;
  }
}
