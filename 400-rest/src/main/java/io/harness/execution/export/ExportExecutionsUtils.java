package io.harness.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class ExportExecutionsUtils {
  public ZonedDateTime prepareZonedDateTime(long epochMillis) {
    if (epochMillis <= 0) {
      return null;
    }

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("UTC"));
  }
}
