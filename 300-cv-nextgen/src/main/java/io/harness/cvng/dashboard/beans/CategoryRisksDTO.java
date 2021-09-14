/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.beans;

import io.harness.cvng.beans.CVMonitoringCategory;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class CategoryRisksDTO {
  long startTimeEpoch;
  long endTimeEpoch;

  List<CategoryRisk> categoryRisks;

  @Builder.Default boolean hasConfigsSetup = true;

  @Data
  @Builder
  public static class CategoryRisk {
    CVMonitoringCategory category;
    Integer risk;
  }
}
