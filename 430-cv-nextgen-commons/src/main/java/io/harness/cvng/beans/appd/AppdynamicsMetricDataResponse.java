package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.ThirdPartyApiResponseStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class AppdynamicsMetricDataResponse {
  private Long startTime;
  private Long endTime;
  private ThirdPartyApiResponseStatus responseStatus;
  private List<DataPoint> dataPoints;

  @Data
  @Builder
  public static class DataPoint {
    long timestamp;
    double value;
  }
}
