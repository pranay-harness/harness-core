package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TimeRange {
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC") Instant startTime;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC") Instant endTime;
}
