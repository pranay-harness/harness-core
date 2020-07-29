package io.harness.cvng.perpetualtask;

import java.util.Map;

public interface CVDataCollectionTaskService {
  String create(String accountId, Map<String, String> params);
  void delete(String accountId, String taskId);
}
