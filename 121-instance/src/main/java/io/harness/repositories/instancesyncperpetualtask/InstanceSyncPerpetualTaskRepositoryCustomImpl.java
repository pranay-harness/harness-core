package io.harness.repositories.instancesyncperpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public class InstanceSyncPerpetualTaskRepositoryCustomImpl implements InstanceSyncPerpetualTaskRepositoryCustom {
  @Override
  public void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds) {
    //    Preconditions.checkArgument(
    //        EmptyPredicate.isNotEmpty(perpetualTaskIds), "perpetualTaskIds must not be empty or null");
    //    Optional<InstanceSyncPerpetualTaskInfo> infoOptional =
    //        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
    //            accountId, infrastructureMappingId);
    //    if (!infoOptional.isPresent()) {
    //      instanceSyncPerpetualTaskRepository.save(InstanceSyncPerpetualTaskInfo.builder()
    //                                                   .accountIdentifier(accountId)
    //                                                   .infrastructureMappingId(infrastructureMappingId)
    //                                                   .perpetualTaskIds(perpetualTaskIds)
    //                                                   .build());
    //    } else {
    //      InstanceSyncPerpetualTaskInfo info = infoOptional.get();
    //      info.getPerpetualTaskIds().addAll(perpetualTaskIds);
    //      instanceSyncPerpetualTaskRepository.save(info);
    //    }
  }
}
