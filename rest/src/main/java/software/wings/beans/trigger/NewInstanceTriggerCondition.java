package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeName("NEW_INSTANCE")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewInstanceTriggerCondition extends TriggerCondition {
  public NewInstanceTriggerCondition() {
    super(TriggerConditionType.NEW_INSTANCE);
  }
}
