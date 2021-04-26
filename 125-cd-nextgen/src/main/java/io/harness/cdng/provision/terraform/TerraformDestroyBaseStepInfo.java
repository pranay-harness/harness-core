package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("terraformDestroyBaseStepInfo")
public class TerraformDestroyBaseStepInfo {
  String provisionerIdentifier;
}
