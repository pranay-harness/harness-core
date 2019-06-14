package software.wings.beans.settings.helm;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import software.wings.annotation.EncryptableSetting;

public interface HelmRepoConfig extends EncryptableSetting, ExecutionCapabilityDemander { String getConnectorId(); }
