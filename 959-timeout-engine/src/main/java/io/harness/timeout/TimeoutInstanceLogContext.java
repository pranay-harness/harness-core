package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

@OwnedBy(CDC)
public class TimeoutInstanceLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(TimeoutInstance.class);

  public TimeoutInstanceLogContext(String timeoutInstanceId, OverrideBehavior behavior) {
    super(ID, timeoutInstanceId, behavior);
  }
}
