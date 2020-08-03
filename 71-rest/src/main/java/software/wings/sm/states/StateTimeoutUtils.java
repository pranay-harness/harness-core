package software.wings.sm.states;

import com.google.common.primitives.Ints;

import io.harness.context.ContextElementType;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.EcsSetupElement;
import software.wings.sm.ExecutionContext;

import java.util.concurrent.TimeUnit;

@Slf4j
public class StateTimeoutUtils {
  private StateTimeoutUtils() {}

  public static Integer getEcsStateTimeoutFromContext(ExecutionContext context) {
    EcsSetupElement ecsSetupElement = context.getContextElement(ContextElementType.ECS_SERVICE_SETUP);
    if (ecsSetupElement == null || ecsSetupElement.getServiceSteadyStateTimeout() == null
        || ecsSetupElement.getServiceSteadyStateTimeout().equals(0)) {
      return null;
    }
    return Ints.checkedCast(TimeUnit.MINUTES.toMillis(ecsSetupElement.getServiceSteadyStateTimeout()));
  }
}
