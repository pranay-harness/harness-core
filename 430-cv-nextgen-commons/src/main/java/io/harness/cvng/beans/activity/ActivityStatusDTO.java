package io.harness.cvng.beans.activity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActivityStatusDTO {
  long durationMs;
  long remainingTimeMs;
  int progressPercentage;
  String activityId;
  ActivityVerificationStatus status;
}
