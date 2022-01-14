package io.harness.cvng.core.beans.params;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
public class TimeRangeParams {
  @NonNull Instant startTime;
  @NonNull Instant endTime;
}
