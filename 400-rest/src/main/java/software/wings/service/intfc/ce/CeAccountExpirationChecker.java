package software.wings.service.intfc.ce;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.annotation.ParametersAreNonnullByDefault;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
@ParametersAreNonnullByDefault
public interface CeAccountExpirationChecker {
  void checkIsCeEnabled(String accountId);
}
