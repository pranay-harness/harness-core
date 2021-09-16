package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@JsonTypeName("NEW_INSTANCE")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewInstanceTriggerCondition extends TriggerCondition {
  public NewInstanceTriggerCondition() {
    super(TriggerConditionType.NEW_INSTANCE);
  }
}
