package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class RemoteTerraformVarFileSpec implements TerraformVarFileSpec {
  @NotNull StoreConfigWrapper store;

  @Override
  public String getType() {
    return TerraformVarFileTypes.Remote;
  }
}
