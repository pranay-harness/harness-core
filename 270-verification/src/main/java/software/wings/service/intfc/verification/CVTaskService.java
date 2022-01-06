/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
