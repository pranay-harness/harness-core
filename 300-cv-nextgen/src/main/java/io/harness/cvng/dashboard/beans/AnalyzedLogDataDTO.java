package io.harness.cvng.dashboard.beans;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
public class AnalyzedLogDataDTO implements Comparable<AnalyzedLogDataDTO> {
  String projectIdentifier;
  String orgIdentifier;
  String environmentIdentifier;
  String serviceIdentifier;

  LogData logData;

  @Data
  @Builder
  public static class LogData {
    String text;
    Long label;
    int count;
    List<FrequencyDTO> trend;
    LogAnalysisTag tag;
  }

  @Data
  @Builder
  public static class FrequencyDTO {
    private long timestamp;
    private int count;
  }

  @Override
  public int compareTo(@NotNull AnalyzedLogDataDTO o) {
    return logData.getLabel().compareTo(o.getLogData().getLabel());
  }
}
