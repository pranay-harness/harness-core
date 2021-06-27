package software.wings.delegatetasks.cv;

import java.time.Duration;

public interface CVConstants {
  Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  int MAX_RETRIES = 2;
  int RATE_LIMIT_STATUS = 429;
  String URL_STRING = "Url";
}
