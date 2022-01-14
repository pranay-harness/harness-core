package io.harness.ccm.views.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewTimeRangeDto {
  long startTime;
  long endTime;
}
