/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;

import java.util.List;
import javax.validation.Valid;
import org.mongodb.morphia.query.Query;

public interface DelegateTaskService {
  void touchExecutingTasks(String accountId, String delegateId, List<String> delegateTaskIds);

  void processDelegateResponse(
      String accountId, String delegateId, String taskId, @Valid DelegateTaskResponse response);

  void handleResponse(DelegateTask delegateTask, Query<DelegateTask> taskQuery, DelegateTaskResponse response);
}
