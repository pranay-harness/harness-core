package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._957_CG_BEANS)
public class AzureContainerRegistry extends AzureResourceReference {
  @Builder
  private AzureContainerRegistry(
      String name, String resourceGroup, String subscriptionId, String type, String id, String loginServer) {
    super(name, resourceGroup, subscriptionId, type, id);
    this.loginServer = loginServer;
  };

  private String loginServer;
}
