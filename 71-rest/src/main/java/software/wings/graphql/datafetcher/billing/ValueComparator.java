package software.wings.graphql.datafetcher.billing;

import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint;

import java.io.Serializable;
import java.util.Comparator;

class ValueComparator implements Comparator<QLSunburstChartDataPoint>, Serializable {
  public int compare(QLSunburstChartDataPoint a, QLSunburstChartDataPoint b) {
    return b.getValue().intValue() - a.getValue().intValue();
  }
}