package io.harness.cvng.core.beans.monitoredService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoredServiceWithHealthSources {
  private String identifier;
  private String name;
  Set<HealthSourceSummary> healthSources;

  @Data
  @Builder
  public static class HealthSourceSummary {
    String name;
    String identifier;
  }
}
