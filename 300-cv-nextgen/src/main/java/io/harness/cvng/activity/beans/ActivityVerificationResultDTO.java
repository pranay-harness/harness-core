/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
public class ActivityVerificationResultDTO {
  private ActivityType activityType;
  private String activityId;
  private String activityName;
  private long activityStartTime;
  private String environmentIdentifier;
  private String environmentName;
  private String serviceIdentifier;
  private long endTime;
  private long remainingTimeMs;
  @Builder.Default private int overallRisk = -1;
  private Set<CategoryRisk> preActivityRisks;
  private Set<CategoryRisk> postActivityRisks;
  int progressPercentage;
  ActivityVerificationStatus status;

  @Data
  @Builder
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  public static class CategoryRisk {
    @EqualsAndHashCode.Include private CVMonitoringCategory category;

    private Double risk;
  }
}
