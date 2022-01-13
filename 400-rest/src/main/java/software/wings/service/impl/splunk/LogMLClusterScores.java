/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
