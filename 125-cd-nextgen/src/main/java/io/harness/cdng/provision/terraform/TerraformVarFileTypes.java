package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface TerraformVarFileTypes {
  String Inline = "Inline";
  String Remote = "Remote";
}
