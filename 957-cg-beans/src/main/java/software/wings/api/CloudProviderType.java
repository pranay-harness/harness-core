package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP) public enum CloudProviderType { PHYSICAL_DATA_CENTER, AWS, AZURE, GCP, KUBERNETES_CLUSTER, PCF, CUSTOM }
