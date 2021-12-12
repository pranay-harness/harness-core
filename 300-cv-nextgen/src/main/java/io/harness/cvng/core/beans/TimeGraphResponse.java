package io.harness.cvng.core.beans;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class TimeGraphResponse {
  @NonNull Long startTime;
  @NonNull Long endTime;
  List<DataPoints> dataPoints;

  @Value
  @Builder
  public static class DataPoints {
    Double value;
    @NonNull Long timeStamp;
  }
}
