package software.wings.beans.settings.helm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import software.wings.annotation.EncryptableSetting;

@OwnedBy(CDC)
public interface HelmRepoConfig extends EncryptableSetting, ExecutionCapabilityDemander {
  String getConnectorId();
}
