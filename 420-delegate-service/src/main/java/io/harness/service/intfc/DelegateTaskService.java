package io.harness.service.intfc;

import java.util.List;

public interface DelegateTaskService {
  void touchExecutingTasks(String accountId, String delegateId, List<String> delegateTaskIds);
}
