package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
@ToString
public class QLCESetupFilter implements EntityFilter {
  QLIdFilter cloudProviderId;
  QLIdFilter infraMasterAccountId;
  QLIdFilter masterAccountSettingId;
}
