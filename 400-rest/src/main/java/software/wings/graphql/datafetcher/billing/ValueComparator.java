/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint;

import java.io.Serializable;
import java.util.Comparator;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
class ValueComparator implements Comparator<QLSunburstChartDataPoint>, Serializable {
  public int compare(QLSunburstChartDataPoint a, QLSunburstChartDataPoint b) {
    return b.getValue().intValue() - a.getValue().intValue();
  }
}
