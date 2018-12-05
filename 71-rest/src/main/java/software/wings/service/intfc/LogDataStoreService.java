package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;

import java.util.List;

public interface LogDataStoreService {
  <T extends GoogleDataStoreAware> void saveLogs(Class<T> clazz, List<T> logs);

  <T extends GoogleDataStoreAware> PageResponse<T> listLogs(Class<T> clazz, PageRequest<T> pageRequest);

  void purgeByActivity(String appId, String activityId);

  void purgeOlderLogs();
}