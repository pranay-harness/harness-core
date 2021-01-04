package io.harness.ccm.billing.graphql;

import lombok.Value;

@Value
public class GcpBillingAccountQueryArguments {
  String uuid;
  String organizationSettingId;
}
