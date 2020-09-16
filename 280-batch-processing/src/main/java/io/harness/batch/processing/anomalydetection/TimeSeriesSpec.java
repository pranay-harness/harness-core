package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TimeSeriesSpec {
  String accountId;
  Instant trainStart;
  Instant trainEnd;
  Instant testStart;
  Instant testEnd;
  TimeGranularity timeGranularity;
  EntityType entityType;
}
