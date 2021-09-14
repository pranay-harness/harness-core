/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLTxnScores {
  private String transactionName;
  private Map<String, TimeSeriesMLMetricScores> scoresMap;
}
