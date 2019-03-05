package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.service.impl.LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DataStoreService;

import java.util.List;

@Singleton
public class MongoDataStoreServiceImpl implements DataStoreService {
  private static final Logger logger = LoggerFactory.getLogger(MongoDataStoreServiceImpl.class);
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
    if (ignoreDuplicate) {
      wingsPersistence.saveIgnoringDuplicateKeys(records);
    } else {
      wingsPersistence.save(records);
    }
  }

  @Override
  public <T extends GoogleDataStoreAware> PageResponse<T> list(Class<T> clazz, PageRequest<T> pageRequest) {
    return wingsPersistence.query(clazz, pageRequest, excludeAuthority);
  }

  @Override
  public void purgeByActivity(String appId, String activityId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Log.class).filter("appId", appId).filter("activityId", activityId));
  }

  @Override
  public void purgeOlderRecords() {
    // do nothing
  }
}