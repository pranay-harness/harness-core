/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import java.util.Map;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 9/24/17.
 */
@Data
public class TimeSeriesMLTxnSummary {
  private String txn_name;
  private String txn_tag;
  private String group_name;
  private boolean is_key_transaction;
  private Map<String, TimeSeriesMLMetricSummary> metrics;
}
