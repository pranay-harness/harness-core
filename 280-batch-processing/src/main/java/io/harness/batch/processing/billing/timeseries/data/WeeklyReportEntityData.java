package io.harness.batch.processing.billing.timeseries.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyReportEntityData {
  String id;
  String parent;
  double cost;
  double costChange;
  double costDiff;
  boolean costDecreased;
}
