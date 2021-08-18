package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum CloudProviderType {
  PHYSICAL_DATA_CENTER,
  AWS,
  AZURE,
  GCP,
  KUBERNETES_CLUSTER,
  PCF,
  CUSTOM
}
