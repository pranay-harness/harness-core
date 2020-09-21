package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LogAnalysisClusterDTO {
  private String message;
  private int label;
  private DeploymentLogAnalysisDTO.ClusterType clusterType;
  private int risk;
  private double score;
  private int count;
  private List<Double> controlFrequencyData;
  private List<Double> testFrequencyData;
}
