/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import software.wings.verification.dashboard.HeatMapUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatMap {
  private CVConfiguration cvConfiguration;
  @Default private List<HeatMapUnit> riskLevelSummary = new ArrayList<>();

  // txn name -> metric name -> time series
  private Map<String, Map<String, List<TimeSeriesDataPoint>>> observedTimeSeries;
  private Map<String, Map<String, List<TimeSeriesDataPoint>>> predictedTimeSeries;
}
