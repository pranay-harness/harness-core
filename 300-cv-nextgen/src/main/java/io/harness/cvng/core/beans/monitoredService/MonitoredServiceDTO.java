package io.harness.cvng.core.beans.monitoredService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoredServiceDTO {
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String identifier;
  @NotNull String name;
  String description;
  @NotNull String serviceRef;
  @NotNull String environmentRef;

  @Valid Sources sources;

  @Data
  @Builder
  public static class Sources {
    @Valid Set<HealthSource> healthSources;

    public Set<HealthSource> getHealthSources() {
      if (healthSources == null) {
        return Collections.emptySet();
      }
      return healthSources;
    }
  }
}
