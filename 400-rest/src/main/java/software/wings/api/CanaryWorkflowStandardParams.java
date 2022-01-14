package software.wings.api;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.WorkflowStandardParams;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(HarnessTeam.CDC)
public class CanaryWorkflowStandardParams extends WorkflowStandardParams {}
