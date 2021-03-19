package software.wings.core.winrm.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.shell.BaseScriptExecutor;

@TargetModule(Module._960_API_SERVICES)
@OwnedBy(CDP)
public interface WinRmExecutor extends BaseScriptExecutor {}
