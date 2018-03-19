package software.wings.service.impl.splunk;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/28/17.
 */
@Data
public class SplunkAnalysisCluster {
  private List<Map> message_frequencies;
  private int cluster_label;
  private List<String> tags;
  private List<Integer> anomalous_counts;
  private boolean unexpected_freq;
  private String text;
  private double x;
  private double y;
  private String feedback_id;
  private List<String> diff_tags;
  private double alert_score;
  private double test_score;
  private double control_score;
  private double freq_score;
  private int control_label;
}
