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
