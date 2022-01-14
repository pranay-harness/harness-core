package io.harness.cvng.beans.activity;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class ActivityStatusDTO {
  long durationMs;
  long remainingTimeMs;
  int progressPercentage;
  @Deprecated String activityId;
  @NonFinal @Setter ActivityVerificationStatus status;
}
