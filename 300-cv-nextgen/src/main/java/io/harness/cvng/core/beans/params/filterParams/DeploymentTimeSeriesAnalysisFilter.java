package io.harness.cvng.core.beans.params.filterParams;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Value
public class DeploymentTimeSeriesAnalysisFilter extends TimeSeriesAnalysisFilter {
  String hostName;

  public boolean filterByHostName() {
    return isNotEmpty(hostName);
  }
}
