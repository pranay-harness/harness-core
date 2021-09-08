package io.harness.ccm.views.entities;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewQueryParams {
  String accountId;
  boolean isTimeTruncGroupByRequired;
  boolean isUsedByTimeSeriesStats; // only true when grid call is made to generate filters for charts
  boolean isClusterQuery;
  boolean isTotalCountQuery; // only true while calculating total number of rows returned by query
  boolean skipRoundOff;
}
