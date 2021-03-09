package io.harness.ccm.views.graphql;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLViewOverviewStatsData {
  Boolean unifiedTableDataPresent;
  Boolean isAwsOrGcpOrClusterConfigured;
}
