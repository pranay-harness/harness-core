package io.harness.service.intfc;

/**
 * Created by rsingh on 10/9/18.
 */
public interface ContinuousVerificationService {
  boolean triggerAPMDataCollection(String accountId);

  /**
   * Creates tasks for Learning Engine
   * @param accountId
   */
  void triggerMetricDataAnalysis(String accountId);

  boolean triggerLogDataCollection(String accountId);

  void triggerLogsL1Clustering(String accountId);

  void triggerLogsL2Clustering(String accountId);

  void triggerLogDataAnalysis(String accountId);

  void cleanupStuckLocks();
}
