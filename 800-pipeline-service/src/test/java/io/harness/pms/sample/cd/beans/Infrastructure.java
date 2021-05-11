package io.harness.pms.sample.cd.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@NoArgsConstructor
public class Infrastructure {
  Environment environment;
  InfrastructureDefinition infrastructureDefinition;
}
