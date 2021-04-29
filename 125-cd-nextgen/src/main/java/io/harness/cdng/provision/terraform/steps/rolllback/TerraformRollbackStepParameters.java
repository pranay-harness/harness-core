package io.harness.cdng.provision.terraform.steps.rolllback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@TypeAlias("TerraformRollbackStepParameters")
@OwnedBy(HarnessTeam.CDP)
public class TerraformRollbackStepParameters implements SpecParameters {
  @NotNull String provisionerIdentifier;
}
