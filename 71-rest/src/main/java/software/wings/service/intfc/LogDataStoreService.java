package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.Log;

import java.util.List;

public interface LogDataStoreService {
  void saveExecutionLog(List<Log> logs);

  PageResponse<Log> listExecutionLog(String appId, PageRequest<Log> pageRequest);

  void purgeByActivity(String appId, String activityId);

  void purgeOlderLogs();
}
