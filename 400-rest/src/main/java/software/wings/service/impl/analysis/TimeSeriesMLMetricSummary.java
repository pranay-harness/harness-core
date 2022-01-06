/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesMLMetricSummary {
  private String metric_name;
  private String metric_type;
  private String alert_type;
  private TimeSeriesMLDataSummary control;
  private TimeSeriesMLDataSummary test;
  private Map<String, TimeSeriesMLHostSummary> results;
  private double control_avg;
  private double test_avg;
  private int max_risk;
  private double max_key_transaction_score;
  private int long_term_pattern;
  private double predictability;
  private long last_seen_time;
  private double txn_relative_risk;
  private boolean should_fail_fast;
}
