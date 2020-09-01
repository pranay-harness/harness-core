package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult;
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

  private List<LogAnalysisCluster> logClusters;
  private List<AnalysisResult> logAnalysisResults;

  public List<LogAnalysisCluster> toAnalysisClusters(
      String cvConfigId, Instant analysisStartTime, Instant analysisEndTime) {
    if (logClusters != null) {
      logClusters.forEach(cluster -> {
        cluster.setCvConfigId(cvConfigId);
        cluster.setAnalysisStartTime(analysisStartTime);
        cluster.setAnalysisEndTime(analysisEndTime);
        cluster.setAccountId(accountId);
        cluster.setAnalysisMinute(analysisMinute);
      });
      return logClusters;
    }
    return null;
  }

  public LogAnalysisResult toAnalysisResult(String cvConfigId, Instant analysisStartTime, Instant analysisEndTime) {
    return LogAnalysisResult.builder()
        .cvConfigId(cvConfigId)
        .analysisStartTime(analysisStartTime)
        .analysisEndTime(analysisEndTime)
        .accountId(accountId)
        .verificationTaskId(cvConfigId)
        .logAnalysisResults(logAnalysisResults)
        .build();
  }
}
