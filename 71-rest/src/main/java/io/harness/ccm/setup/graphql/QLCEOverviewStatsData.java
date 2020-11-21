package io.harness.ccm.setup.graphql;

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
public class QLCEOverviewStatsData {
  Boolean ceEnabledClusterPresent;
  Boolean cloudConnectorsPresent;
  Boolean awsConnectorsPresent;
  Boolean gcpConnectorsPresent;
  Boolean applicationDataPresent;
  Boolean clusterDataPresent;
  @Builder.Default Boolean isSampleClusterPresent = false;
}
