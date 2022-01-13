/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBillingStackedTimeSeriesData implements QLData {
  List<QLBillingStackedTimeSeriesDataPoint> data;
  List<QLBillingStackedTimeSeriesDataPoint> cpuIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> memoryIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> cpuUtilMetrics;
  List<QLBillingStackedTimeSeriesDataPoint> memoryUtilMetrics;
  List<QLBillingStackedTimeSeriesDataPoint> cpuUtilValues;
  List<QLBillingStackedTimeSeriesDataPoint> memoryUtilValues;
  List<QLBillingStackedTimeSeriesDataPoint> cpuRequest;
  List<QLBillingStackedTimeSeriesDataPoint> cpuLimit;
  List<QLBillingStackedTimeSeriesDataPoint> memoryRequest;
  List<QLBillingStackedTimeSeriesDataPoint> memoryLimit;
  String label;
  String info;
}
