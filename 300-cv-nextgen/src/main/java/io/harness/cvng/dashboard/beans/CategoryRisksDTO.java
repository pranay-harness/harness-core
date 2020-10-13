package io.harness.cvng.dashboard.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CategoryRisksDTO {
  long startTimeEpoch;
  long endTimeEpoch;

  List<CategoryRisk> categoryRisks;

  @Data
  @Builder
  public static class CategoryRisk {
    CVMonitoringCategory category;
    Integer risk;
  }
}
