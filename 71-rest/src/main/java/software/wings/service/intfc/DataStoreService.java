package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;

import java.util.List;

public interface DataStoreService {
  <T extends GoogleDataStoreAware> void save(Class<T> clazz, List<T> records, boolean ignoreDuplicate);

  <T extends GoogleDataStoreAware> PageResponse<T> list(Class<T> clazz, PageRequest<T> pageRequest);

  void purgeByActivity(String appId, String activityId);

  void purgeOlderRecords();
}