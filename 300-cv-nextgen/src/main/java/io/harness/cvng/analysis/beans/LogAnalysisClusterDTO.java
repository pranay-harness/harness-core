package io.harness.cvng.analysis.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;

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
