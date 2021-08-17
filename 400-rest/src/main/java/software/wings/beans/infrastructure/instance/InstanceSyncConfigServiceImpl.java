package software.wings.beans.infrastructure.instance;

import com.google.inject.Inject;
import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import software.wings.beans.User;
import software.wings.beans.infrastructure.instance.InstanceSyncConfig;
import software.wings.dl.WingsPersistence;

//@AllArgsConstructor(onConstructor = @__({ @Inject}))
public class InstanceSyncConfigServiceImpl implements InstanceSyncConfigService {
  @Inject private WingsPersistence wingsPersistence;
  private boolean delete;
//  private final InstanceSyncConfigRepository instanceSyncConfigRepository;

  public InstanceSyncConfig save(String accountId, String perpetualTaskType, int timeInterval) {
//    if (getByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType) != null) {
//      Criteria criteria = Criteria.where(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
//                              .is(accountId)
//                              .and(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
//                              .is(perpetualTaskType);
//      Update update = new Update().set(InstanceSyncConfig.InstanceSyncConfigKeys.timeInterval, timeInterval);
//      return instanceSyncConfigRepository.modify(criteria,update);
//    }
//    InstanceSyncConfig instanceSyncConfig=InstanceSyncConfig.builder().accountId(accountId).perpetualTaskType(perpetualTaskType).timeInterval(timeInterval).build();
//    return instanceSyncConfigRepository.save(instanceSyncConfig);

    Query<InstanceSyncConfig> query=wingsPersistence.createQuery(InstanceSyncConfig.class).field(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
            .equal(accountId)
            .field(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
            .equal(perpetualTaskType);
    UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(InstanceSyncConfig.class);
    updateOperations.set(InstanceSyncConfig.InstanceSyncConfigKeys.timeInterval,timeInterval);
    //wingsPersistence.save()
    return wingsPersistence.findAndModify(query,updateOperations,new FindAndModifyOptions().upsert(true).returnNew(true));
  }

  @Override
  public InstanceSyncConfig getByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType) {
//    Criteria criteria = Criteria.where(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
//                            .is(accountId)
//                            .and(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
//                            .is(perpetualTaskType);
return wingsPersistence.createQuery(InstanceSyncConfig.class)
    .field(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
    .equal(accountId)
    .field(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
    .equal(perpetualTaskType)
    .get();
//    return instanceSyncConfigRepository.findByAccountIdAndPerpetualTaskType(accountId,perpetualTaskType);
  }

  public boolean deleteByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType){
//    Criteria criteria = Criteria.where(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
//            .is(accountId)
//            .and(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
//            .is(perpetualTaskType);
    Query<InstanceSyncConfig> query=wingsPersistence.createQuery(InstanceSyncConfig.class).field(InstanceSyncConfig.InstanceSyncConfigKeys.accountId)
            .equal(accountId)
            .field(InstanceSyncConfig.InstanceSyncConfigKeys.perpetualTaskType)
            .equal(perpetualTaskType);
    return wingsPersistence.delete(query);
//    instanceSyncConfigRepository.deleteByAccountIdAndPerpetualTaskType(accountId,perpetualTaskType);
  }
}
