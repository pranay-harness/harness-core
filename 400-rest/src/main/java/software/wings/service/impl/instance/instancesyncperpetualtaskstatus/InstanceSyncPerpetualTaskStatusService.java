package software.wings.service.impl.instance.instancesyncperpetualtaskstatus;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public interface InstanceSyncPerpetualTaskStatusService {
  boolean handlePerpetualTaskFailure(String perpetualTaskId, String errorMessage);

  void updatePerpetualTaskSuccess(String perpetualTaskId);
}
