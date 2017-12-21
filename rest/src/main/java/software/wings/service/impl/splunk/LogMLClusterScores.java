package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

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
