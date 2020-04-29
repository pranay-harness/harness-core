package software.wings.service.impl;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.impl.LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Log;
import software.wings.beans.Log.LogKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DataStoreService;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class MongoDataStoreServiceImpl implements DataStoreService {
  private WingsPersistence wingsPersistence;

  @Inject
  public MongoDataStoreServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public <T extends GoogleDataStoreAware> void save(Class<T> clazz, List<T> records, boolean ignoreDuplicate) {
    if (isEmpty(records)) {
      return;
    }
    if (records.get(0) instanceof Log) {
      Log log = (Log) records.get(0);
      long count = wingsPersistence.createQuery(Log.class)
                       .filter(LogKeys.appId, log.getAppId())
                       .filter(LogKeys.activityId, log.getActivityId())
                       .count();
      if (count >= MAX_LOG_ROWS_PER_ACTIVITY) {
        logger.warn(
            "Number of log rows per activity threshold [{}] crossed. [{}] log lines truncated for activityId: [{}], commandUnitName: [{}]",
            MAX_LOG_ROWS_PER_ACTIVITY, log.getLinesCount(), log.getActivityId(), log.getCommandUnitName());
        return;
      }
    }
    if (ignoreDuplicate) {
      wingsPersistence.saveIgnoringDuplicateKeys(records);
    } else {
      wingsPersistence.save(records);
    }
  }

  @Override
  public void delete(Class<? extends GoogleDataStoreAware> clazz, String id) {
    Query<? extends GoogleDataStoreAware> query = wingsPersistence.createQuery(clazz).filter("_id", id);
    wingsPersistence.delete(query);
  }

  @Override
  public <T extends GoogleDataStoreAware> PageResponse<T> list(Class<T> clazz, PageRequest<T> pageRequest) {
    PageResponse<T> response = wingsPersistence.query(clazz, pageRequest, excludeAuthority);

    if (isNotEmpty(pageRequest.getLimit()) && pageRequest.getLimit().equals(UNLIMITED)) {
      int previousOffset = 0;
      List<T> responseList = new ArrayList<>();
      while (!response.isEmpty()) {
        responseList.addAll(response.getResponse());
        previousOffset += response.size();
        pageRequest.setOffset(String.valueOf(previousOffset));
        response = wingsPersistence.query(clazz, pageRequest, excludeAuthority);
      }
      response.setResponse(responseList);
      response.setOffset(String.valueOf(responseList.size()));
    }
    return response;
  }

  @Override
  public <T extends GoogleDataStoreAware> T getEntity(Class<T> clazz, String id) {
    return wingsPersistence.get(clazz, id);
  }

  @Override
  public <T extends GoogleDataStoreAware> void incrementField(
      Class<T> clazz, String id, String fieldName, int incrementCount) {
    UpdateOperations<T> updateOps = wingsPersistence.createUpdateOperations(clazz).inc(fieldName, incrementCount);
    Query<T> query = wingsPersistence.createQuery(clazz).filter(ID_KEY, id);
    wingsPersistence.findAndModify(query, updateOps, new FindAndModifyOptions());
  }

  @Override
  public void purgeByActivity(String appId, String activityId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Log.class).filter("appId", appId).filter(LogKeys.activityId, activityId));
  }

  @Override
  public void purgeOlderRecords() {
    // do nothing
  }
}