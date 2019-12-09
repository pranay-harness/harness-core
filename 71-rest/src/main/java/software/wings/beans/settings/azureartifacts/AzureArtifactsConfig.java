package software.wings.beans.settings.azureartifacts;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import software.wings.annotation.EncryptableSetting;

public interface AzureArtifactsConfig extends EncryptableSetting, ExecutionCapabilityDemander {
  String getAzureDevopsUrl();
}
