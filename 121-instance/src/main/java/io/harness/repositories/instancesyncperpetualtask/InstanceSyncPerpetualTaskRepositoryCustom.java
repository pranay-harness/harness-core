package io.harness.repositories.instancesyncperpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskRepositoryCustom {
  InstanceSyncPerpetualTaskInfo update(Criteria criteria, Update update);
}
