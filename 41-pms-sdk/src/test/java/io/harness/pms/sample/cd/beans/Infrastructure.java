package io.harness.pms.sample.cd.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Infrastructure {
  Environment environment;
  InfrastructureDefinition infrastructureDefinition;
}
