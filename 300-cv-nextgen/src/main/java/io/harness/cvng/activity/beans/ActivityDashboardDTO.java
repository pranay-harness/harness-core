package io.harness.cvng.activity.beans;

import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityDashboardDTO {
  ActivityType activityType;
  String activityId;
  String activityName;
  long activityStartTime;
  String environmentIdentifier;
  String environmentName;
  String serviceIdentifier;
  ActivityVerificationStatus verificationStatus;
  ActivityVerificationSummary activityVerificationSummary;
}
