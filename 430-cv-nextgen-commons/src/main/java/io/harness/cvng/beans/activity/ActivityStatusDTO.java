/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  String activityId;
  @NonFinal @Setter ActivityVerificationStatus status;
}
