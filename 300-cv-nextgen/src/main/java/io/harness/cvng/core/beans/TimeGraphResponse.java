package io.harness.cvng.core.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
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
