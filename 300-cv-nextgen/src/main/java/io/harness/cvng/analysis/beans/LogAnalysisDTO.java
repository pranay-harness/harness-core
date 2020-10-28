package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult;
import io.harness.mongo.index.FdIndex;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class LogAnalysisDTO {
  @Setter private String verificationTaskId;
  @Setter private Instant analysisStartTime;
  @Setter private Instant analysisEndTime;
  @FdIndex private String accountId;
  private String analysisSummaryMessage;
  private double score;
  private long analysisMinute;

  private List<LogAnalysisCluster> logClusters;
  private List<AnalysisResult> logAnalysisResults;

  public List<LogAnalysisCluster> toAnalysisClusters(
      String verificationTaskId, Instant analysisStartTime, Instant analysisEndTime) {
    if (logClusters != null) {
      logClusters.forEach(cluster -> {
        cluster.setVerificationTaskId(verificationTaskId);
        cluster.setAnalysisStartTime(analysisStartTime);
        cluster.setAnalysisEndTime(analysisEndTime);
        cluster.setAccountId(accountId);
        cluster.setAnalysisMinute(analysisMinute);
      });
      return logClusters;
    }
    return null;
  }

  public LogAnalysisResult toAnalysisResult(
      String verificationTaskId, Instant analysisStartTime, Instant analysisEndTime) {
    return LogAnalysisResult.builder()
        .analysisStartTime(analysisStartTime)
        .analysisEndTime(analysisEndTime)
        .accountId(accountId)
        .score(score)
        .verificationTaskId(verificationTaskId)
        .logAnalysisResults(logAnalysisResults)
        .build();
  }
}
