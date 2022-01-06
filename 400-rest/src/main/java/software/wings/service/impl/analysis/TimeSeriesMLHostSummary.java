/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 9/24/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesMLHostSummary {
  private List<Double> distance;
  private List<Double> control_data;
  private List<Double> test_data;
  private List<Double> history;
  private List<Double> history_bins;
  private List<Double> all_risks;
  private List<Character> control_cuts;
  private List<Character> test_cuts;
  private String optimal_cuts;
  private List<Double> optimal_data;
  private int control_index;
  private int test_index;
  private String nn;
  private int risk;
  private double score;
  private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType;
  private List<Integer> anomalies;
  private List<Long> anomalousTimeStamps;
  @JsonProperty("upper_threshold") private List<Double> upperThreshold;
  @JsonProperty("lower_threshold") private List<Double> lowerThreshold;
  @JsonProperty("host_name") private String hostName;
  @JsonProperty("fail_fast_criteria_description") private String failFastCriteriaDescription;
}
