package software.wings.delegatetasks.cv;

import com.google.common.annotations.VisibleForTesting;

import java.time.Duration;

public class CVConstants {
  private CVConstants() {}
  @VisibleForTesting static Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
}
