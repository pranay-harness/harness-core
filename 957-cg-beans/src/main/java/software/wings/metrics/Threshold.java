/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 10/11/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Threshold {
  private ThresholdType thresholdType;
  private ThresholdComparisonType comparisonType;
  private double ml;
  private TimeSeriesCustomThresholdType customThresholdType = TimeSeriesCustomThresholdType.ACCEPTABLE;
  // the thresholdCriteria will be present for Anomalous thresholds.
  private TimeSeriesCustomThresholdCriteria thresholdCriteria;

  public boolean isSimilarTo(Threshold other) {
    if (this.thresholdType == other.getThresholdType() && this.comparisonType == other.getComparisonType()
        && this.customThresholdType == other.getCustomThresholdType()
        && this.thresholdCriteria == other.getThresholdCriteria()) {
      return true;
    }
    return false;
  }
}
