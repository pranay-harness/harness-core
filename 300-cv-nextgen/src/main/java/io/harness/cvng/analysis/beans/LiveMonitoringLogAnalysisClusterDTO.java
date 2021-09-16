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
      if (logAnalysisTag.equals(LogAnalysisTag.UNKNOWN) || logAnalysisTag.equals(LogAnalysisTag.UNEXPECTED)) {
        this.risk(Risk.HIGH);
      } else {
        this.risk(Risk.LOW);
      }

      return this;
    }
  }
}
