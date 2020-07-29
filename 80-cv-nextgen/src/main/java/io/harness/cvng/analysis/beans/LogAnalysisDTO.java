package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.LogAnalysisFrequencyPattern.FrequencyPattern;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.mongo.index.FdIndex;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class LogAnalysisDTO {
  private String cvConfigId;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  @FdIndex private String accountId;
  private String analysisSummaryMessage;
  private double score;
  private long analysisMinute;

  private List<List<LogAnalysisCluster>> unknownEvents;
  private List<LogAnalysisCluster> testEvents;
  private List<LogAnalysisCluster> controlEvents;
  private List<LogAnalysisCluster> controlClusters;
  private List<LogAnalysisCluster> unknownClusters;
  private List<LogAnalysisCluster> testClusters;

  private List<FrequencyPattern> frequencyPatterns;

  public LogAnalysisRecord toAnalysisRecord() {
    return LogAnalysisRecord.builder()
        .accountId(accountId)
        .cvConfigId(cvConfigId)
        .analysisStartTime(analysisStartTime)
        .analysisEndTime(analysisEndTime)
        .analysisSummaryMessage(analysisSummaryMessage)
        .score(score)
        .analysisMinute(analysisMinute)
        .unknownClusters(unknownClusters)
        .controlClusters(controlClusters)
        .testClusters(testClusters)
        .unknownEvents(unknownEvents)
        .testEvents(testEvents)
        .controlEvents(controlEvents)
        .build();
  }
}
