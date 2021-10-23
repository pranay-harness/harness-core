package io.harness.cvng.servicelevelobjective.beans;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class ServiceLevelIndicator {
  @NonNull String name;
  @NonNull String identifier;
  @NonNull SLIType type;
  @NonNull SLISpec spec;

  @Data
  @Builder
  public static class SLISpec {
    @NonNull SLIMetricType type;
    @NonNull SLIMetricSpec spec;

    @Data
    @Builder
    public static class SLIMetricSpec {
      @NonNull String eventType;
      @NonNull String metric1;
      @NonNull String metric2;
    }
  }
}
