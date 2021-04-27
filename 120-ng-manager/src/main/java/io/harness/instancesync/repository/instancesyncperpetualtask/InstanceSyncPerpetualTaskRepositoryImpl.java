package io.harness.instancesync.repository.instancesyncperpetualtask;

import static io.harness.persistence.HQuery.excludeAuthority;

import static java.util.Optional.ofNullable;

import io.harness.data.structure.EmptyPredicate;
import io.harness.instancesync.dto.InstanceSyncPerpetualTaskInfo;

import software.wings.dl.WingsPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.mongodb.morphia.query.Query;

public class InstanceSyncPerpetualTaskRepositoryImpl implements InstanceSyncPerpetualTaskRepository {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds) {
    Preconditions.checkArgument(
        EmptyPredicate.isNotEmpty(perpetualTaskIds), "perpetualTaskIds must not be empty or null");
    Optional<InstanceSyncPerpetualTaskInfo> infoOptional =
        getByAccountIdAndInfrastructureMappingId(accountId, infrastructureMappingId);
    if (!infoOptional.isPresent()) {
      save(InstanceSyncPerpetualTaskInfo.builder()
               .accountId(accountId)
               .infrastructureMappingId(infrastructureMappingId)
               .perpetualTaskIds(perpetualTaskIds)
               .build());
    } else {
      InstanceSyncPerpetualTaskInfo info = infoOptional.get();
      info.getPerpetualTaskIds().addAll(perpetualTaskIds);
      save(info);
    }
  }

  public void save(InstanceSyncPerpetualTaskInfo info) {
    wingsPersistence.save(info);
  }

  public void delete(String infrastructureMappingId) {
    Query<InstanceSyncPerpetualTaskInfo> deleteQuery =
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class, excludeAuthority)
            .filter(InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId, infrastructureMappingId);

    wingsPersistence.delete(deleteQuery);
  }

  public Optional<InstanceSyncPerpetualTaskInfo> getByAccountIdAndInfrastructureMappingId(
      String accountId, String infrastructureMappingId) {
    Query<InstanceSyncPerpetualTaskInfo> query = getInfoQuery(accountId, infrastructureMappingId);
    return ofNullable(query.get());
  }

  // ---------------------------------- PRIVATE METHODS ----------------------------------

  private Query<InstanceSyncPerpetualTaskInfo> getInfoQuery(String accountId, String infrastructureMappingId) {
    return wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
        .filter(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.accountId, accountId)
        .filter(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId,
            infrastructureMappingId);
  }
}
