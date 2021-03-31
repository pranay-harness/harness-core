package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint;

import java.io.Serializable;
import java.util.Comparator;

@OwnedBy(CE)
class ValueComparator implements Comparator<QLSunburstChartDataPoint>, Serializable {
  public int compare(QLSunburstChartDataPoint a, QLSunburstChartDataPoint b) {
    return b.getValue().intValue() - a.getValue().intValue();
  }
}
