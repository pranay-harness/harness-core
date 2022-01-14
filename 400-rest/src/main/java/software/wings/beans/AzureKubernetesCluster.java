package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class AzureKubernetesCluster extends AzureResourceReference {
  @Builder
  private AzureKubernetesCluster(String name, String resourceGroup, String subscriptionId, String type, String id) {
    super(name, resourceGroup, subscriptionId, type, id);
  };
}
