/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
