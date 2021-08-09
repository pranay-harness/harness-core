package io.harness.cvng.activity.beans;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityVerificationSummary {
  int total;
  int passed;
  int failed;
  int errors;
  int progress;
  int notStarted;
  int aborted;
  long remainingTimeMs;
  int progressPercentage;
  Long startTime;
  Long durationMs;
  Risk risk;

  public ActivityVerificationStatus getAggregatedStatus() {
    if (total == passed) {
      return ActivityVerificationStatus.VERIFICATION_PASSED;
    } else if (progress > 0) {
      return ActivityVerificationStatus.IN_PROGRESS;
    } else if (errors > 0) {
      return ActivityVerificationStatus.ERROR;
    } else if (failed > 0) {
      return ActivityVerificationStatus.VERIFICATION_FAILED;
    } else if (aborted > 0) {
      return ActivityVerificationStatus.ABORTED;
    }
    return ActivityVerificationStatus.NOT_STARTED;
  }
}
