package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;
@Value
@Builder
public class DeploymentLogAnalysisDTO {
  List<Cluster> clusters;
  ResultSummary resultSummary;
  List<HostSummary> hostSummaries;
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
  }
  @Value
  @Builder
  public static class ResultSummary {
    int risk;
    double score;
    List<Integer> controlClusterLabels;
    List<ClusterSummary> testClusterSummaries;
  }
  @Value
  @Builder
  public static class HostSummary {
    String host;
    ResultSummary resultSummary;
  }
}
