package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@OwnedBy(DEL)
@UtilityClass
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class PerpetualTaskScheduleConstants {
  public static final String PERPETUAL_TASK_SCHEDULE_FLOW = "perpetualTaskScheduleFlow";
}
