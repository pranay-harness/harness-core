package software.wings.beans.infrastructure.instance;

import software.wings.beans.infrastructure.instance.PerpetualTaskScheduleConfig.PerpetualTaskScheduleConfigKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class InstanceSyncConfigServiceImpl implements InstanceSyncConfigService {
  @Inject private WingsPersistence wingsPersistence;

  public PerpetualTaskScheduleConfig save(String accountId, String perpetualTaskType, int timeInterval) {
    Query<PerpetualTaskScheduleConfig> query = wingsPersistence.createQuery(PerpetualTaskScheduleConfig.class)
                                          .field(PerpetualTaskScheduleConfigKeys.accountId)
                                          .equal(accountId)
                                          .field(PerpetualTaskScheduleConfigKeys.perpetualTaskType)
                                          .equal(perpetualTaskType);
    UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(PerpetualTaskScheduleConfig.class);
    updateOperations.set(PerpetualTaskScheduleConfigKeys.timeInterval, timeInterval);
    return wingsPersistence.findAndModify(
        query, updateOperations, new FindAndModifyOptions().upsert(true).returnNew(true));
  }

  @Override
  public PerpetualTaskScheduleConfig getByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType) {
    return wingsPersistence.createQuery(PerpetualTaskScheduleConfig.class)
        .field(PerpetualTaskScheduleConfigKeys.accountId)
        .equal(accountId)
        .field(PerpetualTaskScheduleConfigKeys.perpetualTaskType)
        .equal(perpetualTaskType)
        .get();
  }

  public boolean deleteByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType) {
    Query<PerpetualTaskScheduleConfig> query = wingsPersistence.createQuery(PerpetualTaskScheduleConfig.class)
                                          .field(PerpetualTaskScheduleConfigKeys.accountId)
                                          .equal(accountId)
                                          .field(PerpetualTaskScheduleConfigKeys.perpetualTaskType)
                                          .equal(perpetualTaskType);
    return wingsPersistence.delete(query);
  }
}
