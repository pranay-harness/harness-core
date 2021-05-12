package io.harness.repositories.instancesyncperpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.entities.InstanceSyncPerpetualTaskInfo;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public class InstanceSyncPerpetualTaskRepositoryCustomImpl implements InstanceSyncPerpetualTaskRepositoryCustom {
  @Inject InstanceSyncPerpetualTaskRepository instanceSyncPerpetualTaskRepository;

  @Override
  public void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds) {
    Preconditions.checkArgument(
        EmptyPredicate.isNotEmpty(perpetualTaskIds), "perpetualTaskIds must not be empty or null");
    Optional<InstanceSyncPerpetualTaskInfo> infoOptional =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
            accountId, infrastructureMappingId);
    if (!infoOptional.isPresent()) {
      instanceSyncPerpetualTaskRepository.save(InstanceSyncPerpetualTaskInfo.builder()
                                                   .accountIdentifier(accountId)
                                                   .infrastructureMappingId(infrastructureMappingId)
                                                   .perpetualTaskIds(perpetualTaskIds)
                                                   .build());
    } else {
      InstanceSyncPerpetualTaskInfo info = infoOptional.get();
      info.getPerpetualTaskIds().addAll(perpetualTaskIds);
      instanceSyncPerpetualTaskRepository.save(info);
    }
  }

  @Override
  public void save(String accountId, String infrastructureMappingId, String perpetualTaskId) {
    List<String> perpetualTaskIdList = new ArrayList<>();
    perpetualTaskIdList.add(perpetualTaskId);
    save(accountId, infrastructureMappingId, perpetualTaskIdList);
  }
}
