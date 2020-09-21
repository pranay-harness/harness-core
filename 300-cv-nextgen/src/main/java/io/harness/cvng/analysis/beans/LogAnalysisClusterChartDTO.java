package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogAnalysisClusterChartDTO {
  private int label;
  String text;
  int risk;
  double x;
  double y;
}
