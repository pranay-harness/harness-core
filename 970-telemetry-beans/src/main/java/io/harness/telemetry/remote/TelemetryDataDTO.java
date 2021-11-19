package io.harness.telemetry.remote;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.telemetry.SegmentEventType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class TelemetryDataDTO {
  private SegmentEventType eventType;
  private Map<String, String> properties;
  private GroupPayloadDTO groupPayloadDTO;
  private IdentifyPayloadDTO identifyPayloadDTO;
  private TrackPayloadDTO trackPayloadDTO;
}
