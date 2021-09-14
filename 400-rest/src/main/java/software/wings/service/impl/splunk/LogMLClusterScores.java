/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.splunk;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 10/23/17.
 */
@Data
public class LogMLClusterScores {
  private Map<String, LogMLScore> unknown;
  private Map<String, LogMLScore> test;

  @Data
  @Builder
  public static class LogMLScore {
    private boolean unexpected_freq;
    private double alert_score;
    private double test_score;
    private double control_score;
    private double freq_score;
  }
}
