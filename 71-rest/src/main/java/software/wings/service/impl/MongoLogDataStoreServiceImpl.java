package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static software.wings.service.impl.LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.ReadPref;
import org.mongodb.morphia.DatastoreImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.LogDataStoreService;

import java.util.ArrayList;
import java.util.List;

public class MongoLogDataStoreServiceImpl implements LogDataStoreService {
  private static final Logger logger = LoggerFactory.getLogger(MongoLogDataStoreServiceImpl.class);
  private WingsPersistence wingsPersistence;

  @Inject
  public MongoLogDataStoreServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public <T extends GoogleDataStoreAware> void saveLogs(Class<T> clazz, List<T> logs) {
    if (isEmpty(logs)) {
      return;
    }
    if (logs.get(0) instanceof Log) {
      Log log = (Log) logs.get(0);
      long count = wingsPersistence.createQuery(Log.class)
                       .filter("appId", log.getAppId())
                       .filter("activityId", log.getActivityId())
                       .count();
      if (count >= MAX_LOG_ROWS_PER_ACTIVITY) {
        logger.warn(
            "Number of log rows per activity threshold [{}] crossed. [{}] log lines truncated for activityId: [{}], commandUnitName: [{}]",
            MAX_LOG_ROWS_PER_ACTIVITY, log.getLinesCount(), log.getActivityId(), log.getCommandUnitName());
        return;
      }
    }
    List<DBObject> dbObjects = new ArrayList<>(logs.size());
    for (T log : logs) {
      try {
        DBObject dbObject =
            ((DatastoreImpl) wingsPersistence.getDatastore(Log.class, ReadPref.NORMAL)).getMapper().toDBObject(log);
        dbObjects.add(dbObject);
        if (dbObjects.size() >= MAX_LOG_ROWS_PER_ACTIVITY) {
          break;
        }
      } catch (Exception e) {
        logger.error(format("Exception in saving log [%s]", log), e);
      }
    }
    wingsPersistence.getCollection(clazz, ReadPref.NORMAL).insert(dbObjects);
  }

  @Override
  public <T extends GoogleDataStoreAware> PageResponse<T> listLogs(Class<T> clazz, PageRequest<T> pageRequest) {
    return wingsPersistence.query(clazz, pageRequest);
  }

  @Override
  public void purgeByActivity(String appId, String activityId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Log.class).filter("appId", appId).filter("activityId", activityId));
  }

  @Override
  public void purgeOlderLogs() {
    // do nothing
  }
}