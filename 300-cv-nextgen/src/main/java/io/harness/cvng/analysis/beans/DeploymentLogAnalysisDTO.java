package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentLogAnalysisDTO {
  List<Cluster> clusters;
  ResultSummary resultSummary;
  List<HostSummary> hostSummaries;
  public List<Cluster> getClusters() {
    if (this.clusters == null) {
      return Collections.emptyList();
    }
    return clusters;
  }

  public List<HostSummary> getHostSummaries() {
    if (this.hostSummaries == null) {
      return Collections.emptyList();
    }
    return hostSummaries;
  }
  @Value
  @Builder
  public static class Cluster {
    String text;
    int label;
    double x, y;
  }
  enum ClusterType { KNOWN_EVENT, UNKNOWN_EVENT, UNEXPECTED_FREQUENCY }
  @Value
  @Builder
  public static class ClusterSummary {
    int label;
    ClusterType clusterType;
    int risk;
    double score;
    int count;
    List<Double> controlFrequencyData;
    List<Double> testFrequencyData;
    public List<Double> getControlFrequencyData() {
      if (this.controlFrequencyData == null) {
        return Collections.emptyList();
      }
      return controlFrequencyData;
    }

    public List<Double> getTestFrequencyData() {
      if (this.testFrequencyData == null) {
        return Collections.emptyList();
      }
      return testFrequencyData;
    }
  }
  @Value
  @Builder
  public static class ResultSummary {
    int risk;
    double score;
    List<Integer> controlClusterLabels;
    List<ClusterSummary> testClusterSummaries;

    public List<Integer> getControlClusterLabels() {
      if (controlClusterLabels == null) {
        return Collections.emptyList();
      }
      return controlClusterLabels;
    }

    public List<ClusterSummary> getTestClusterSummaries() {
      if (testClusterSummaries == null) {
        return Collections.emptyList();
      }
      return testClusterSummaries;
    }
  }
  @Value
  @Builder
  public static class HostSummary {
    String host;
    ResultSummary resultSummary;
  }
}
