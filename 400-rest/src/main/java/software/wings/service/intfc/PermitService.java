package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Permit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface PermitService {
  String acquirePermit(@Valid Permit permit);
  boolean releasePermitByKey(@NotNull String key);
}
