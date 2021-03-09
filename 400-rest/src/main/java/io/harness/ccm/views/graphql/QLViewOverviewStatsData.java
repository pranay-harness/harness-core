package io.harness.ccm.views.graphql;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLViewOverviewStatsData {
  Boolean unifiedTableDataPresent;
  Boolean isAwsOrGcpOrClusterConfigured;
}
