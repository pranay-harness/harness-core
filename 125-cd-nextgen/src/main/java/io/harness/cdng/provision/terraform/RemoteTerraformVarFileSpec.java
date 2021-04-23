package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class RemoteTerraformVarFileSpec implements TerraformVarFileSpec {
  @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;

  @Override
  public String getType() {
    return "Remote";
  }
}
