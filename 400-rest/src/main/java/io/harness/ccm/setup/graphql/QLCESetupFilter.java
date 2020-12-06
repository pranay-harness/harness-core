package io.harness.ccm.setup.graphql;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
public class QLCESetupFilter implements EntityFilter {
  QLIdFilter cloudProviderId;
  QLIdFilter infraMasterAccountId;
  QLIdFilter masterAccountSettingId;
  QLIdFilter settingId;
}
