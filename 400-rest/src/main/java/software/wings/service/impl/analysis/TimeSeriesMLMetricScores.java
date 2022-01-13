/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLMetricScores {
  private String metricName;
  private List<Double> scores;
}
