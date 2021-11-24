package software.wings.security.authentication.totp;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class RateLimitProtection {
  @Builder.Default List<Long> incorrectAttemptTimestamps = new ArrayList<>();
  long lastNotificationSentToUserAt;
  long lastNotificationSentToSecOpsAt;
  int totalIncorrectAttempts;
}
