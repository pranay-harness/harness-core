/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import software.wings.beans.Account;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;

/**
 * Created by rsingh on 10/9/18.
 */
public interface ContinuousVerificationService {
  boolean shouldPerformServiceGuardTasks(Account account);
  boolean triggerAPMDataCollection(String accountId);

  /**
   * Creates tasks for Learning Engine
   * @param accountId
   */
  void triggerServiceGuardTimeSeriesAnalysis(String accountId);

  boolean triggerLogDataCollection(String accountId);

  void triggerLogsL1Clustering(String accountId);

  void triggerLogsL2Clustering(String accountId);

  void triggerLogDataAnalysis(String accountId);

  void triggerFeedbackAnalysis(String accountId);

  void cleanupStuckLocks();

  boolean triggerWorkflowDataCollection(AnalysisContext context);

  void markWorkflowDataCollectionDone(AnalysisContext context);

  void triggerTimeSeriesAlertIfNecessary(String cvConfigId, double riskScore, long analysisMinute);

  void triggerLogAnalysisAlertIfNecessary(
      String cvConfigId, LogMLAnalysisRecord mlAnalysisResponse, int analysisMinute);

  void processNextCVTasks(String accountId);

  void expireLongRunningCVTasks(String accountId);

  void retryCVTasks(String accountId);
}
