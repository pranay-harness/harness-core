package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@JsonTypeName("NEW_INSTANCE")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewInstanceTriggerCondition extends TriggerCondition {
  public NewInstanceTriggerCondition() {
    super(TriggerConditionType.NEW_INSTANCE);
  }
}
