package io.harness.ccm.billing.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@Value
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class GcpBillingAccountQueryArguments {
  String uuid;
  String organizationSettingId;
}
