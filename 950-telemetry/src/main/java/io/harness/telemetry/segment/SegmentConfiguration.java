package io.harness.telemetry.segment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.TelemetryConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentConfiguration implements TelemetryConfiguration {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  private String apiKey;
  private boolean certValidationRequired;
}
