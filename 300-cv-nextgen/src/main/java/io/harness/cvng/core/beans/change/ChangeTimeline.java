package io.harness.cvng.core.beans.change;

import io.harness.cvng.beans.change.ChangeCategory;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ChangeTimeline {
  @Singular("categoryTimeline") Map<ChangeCategory, List<TimeRangeDetail>> categoryTimeline;

  @Value
  @Builder
  public static class TimeRangeDetail {
    Long count;
    Long startTime;
    Long endTime;
  }
}
