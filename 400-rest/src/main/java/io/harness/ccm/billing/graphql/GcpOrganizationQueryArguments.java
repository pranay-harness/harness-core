package io.harness.ccm.billing.graphql;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@Value
@TargetModule(Module._490_CE_COMMONS)
public class GcpOrganizationQueryArguments {
  private String uuid;
}
