package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum InstanceSyncFlow {
  NEW_DEPLOYMENT,
  PERPETUAL_TASK,
  ITERATOR,
  MANUAL
}
