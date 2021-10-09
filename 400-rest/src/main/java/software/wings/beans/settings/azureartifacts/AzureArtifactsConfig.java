package software.wings.beans.settings.azureartifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;

import software.wings.annotation.EncryptableSetting;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public interface AzureArtifactsConfig extends EncryptableSetting, ExecutionCapabilityDemander {
  String getAzureDevopsUrl();
}
