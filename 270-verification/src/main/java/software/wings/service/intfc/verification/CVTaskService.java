/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.verification;

import io.harness.entities.CVTask;

import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.util.List;
import java.util.Optional;

public interface CVTaskService {
  void saveCVTask(CVTask cvTask);

  void createCVTasks(AnalysisContext context);

  void enqueueSequentialTasks(List<CVTask> cvTasks);

  Optional<CVTask> getNextTask(String accountId);

  CVTask getCVTask(String cvTaskId);

  void updateTaskStatus(String cvTaskId, DataCollectionTaskResult result);

  void expireLongRunningTasks(String accountId);

  void retryTasks(String accountId);
}
