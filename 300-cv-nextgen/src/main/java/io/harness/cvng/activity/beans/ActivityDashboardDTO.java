/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.beans;

import io.harness.cvng.beans.activity.ActivityType;
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
