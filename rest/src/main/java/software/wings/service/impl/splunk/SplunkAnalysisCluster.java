package software.wings.service.impl.splunk;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/28/17.
 */
public class SplunkAnalysisCluster {
  private List<Map> message_frequencies;
  private int cluster_label;
  private List<String> tags;
  private List<Integer> anomalous_counts;
  private boolean unexpected_freq;
  private String text;
  private double x;
  private double y;

  public List<Map> getMessage_frequencies() {
    return message_frequencies;
  }

  public void setMessage_frequencies(List<Map> message_frequencies) {
    this.message_frequencies = message_frequencies;
  }

  public int getCluster_label() {
    return cluster_label;
  }

  public void setCluster_label(int cluster_label) {
    this.cluster_label = cluster_label;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<Integer> getAnomalous_counts() {
    return anomalous_counts;
  }

  public void setAnomalous_counts(List<Integer> anomalous_counts) {
    this.anomalous_counts = anomalous_counts;
  }

  public boolean isUnexpected_freq() {
    return unexpected_freq;
  }

  public void setUnexpected_freq(boolean unexpected_freq) {
    this.unexpected_freq = unexpected_freq;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = y;
  }

  @Override
  public String toString() {
    return "SplunkAnalysisCluster{"
        + "message_frequencies=" + message_frequencies + ", cluster_label=" + cluster_label + ", tags=" + tags
        + ", anomalous_counts=" + anomalous_counts + ", unexpected_freq=" + unexpected_freq + ", text='" + text + '\''
        + ", x=" + x + ", y=" + y + '}';
  }
}
