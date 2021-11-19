package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LiveMonitoringLogAnalysisClusterDTO {
  String text;
  Risk risk;
  double x;
  double y;
  LogAnalysisTag tag;

  public static class LiveMonitoringLogAnalysisClusterDTOBuilder {
    public LiveMonitoringLogAnalysisClusterDTOBuilder tag(LogAnalysisTag logAnalysisTag) {
      this.tag = logAnalysisTag;
      if (LogAnalysisTag.getAnomalousTags().contains(logAnalysisTag)) {
        this.risk(Risk.UNHEALTHY);
      } else {
        this.risk(Risk.HEALTHY);
      }
      return this;
    }
  }
}
