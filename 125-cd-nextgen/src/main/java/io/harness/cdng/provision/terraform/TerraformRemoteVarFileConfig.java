package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class TerraformRemoteVarFileConfig implements TerraformVarFileConfig {
  GitStoreConfigDTO gitStoreConfigDTO;
}
