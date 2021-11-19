package io.harness.ccm.views.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "The time interval on which you want to create a Perspective")
public class ViewTimeRange {
  ViewTimeRangeType viewTimeRangeType;
  @Nullable long startTime;
  @Nullable long endTime;
}
