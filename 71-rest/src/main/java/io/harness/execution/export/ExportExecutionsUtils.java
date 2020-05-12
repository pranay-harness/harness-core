package io.harness.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@OwnedBy(CDC)
@UtilityClass
public class ExportExecutionsUtils {
  public ZonedDateTime prepareZonedDateTime(long epochMillis) {
    if (epochMillis <= 0) {
      return null;
    }

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
  }
}
