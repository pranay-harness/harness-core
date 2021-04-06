package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QLInfraType {
  QLInfraTypesEnum infraType;
}
