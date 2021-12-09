package io.harness.telemetry;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SegmentEventType {
  @JsonProperty("track") TRACK,
  @JsonProperty("group") GROUP,
  @JsonProperty("identify") IDENTIFY
}
