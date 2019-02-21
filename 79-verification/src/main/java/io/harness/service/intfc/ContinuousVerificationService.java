package io.harness.service.intfc;

import software.wings.service.impl.analysis.AnalysisContext;

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

  boolean triggerLogDataCollection(AnalysisContext context);
}
