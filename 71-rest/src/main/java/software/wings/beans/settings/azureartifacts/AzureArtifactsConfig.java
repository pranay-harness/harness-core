package software.wings.beans.settings.azureartifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import software.wings.annotation.EncryptableSetting;

@OwnedBy(CDC)
public interface AzureArtifactsConfig extends EncryptableSetting, ExecutionCapabilityDemander {
  String getAzureDevopsUrl();
}
