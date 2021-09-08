package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogAnalysisClusterChartDTO {
  private int label;
  String text;
  String hostName;
  Risk risk;
  double x;
  double y;
  DeploymentLogAnalysisDTO.ClusterType clusterType;
}
