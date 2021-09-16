package io.harness.cvng.core.beans.params.filterParams;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Data
public class TimeSeriesAnalysisFilter extends AnalysisFilter {
  boolean anomalous;
}
