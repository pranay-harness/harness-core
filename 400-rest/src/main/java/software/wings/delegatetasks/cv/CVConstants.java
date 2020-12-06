package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;

@TargetModule(Module._930_DELEGATE_TASKS)
public class CVConstants {
  private CVConstants() {}
  @VisibleForTesting static Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
}
