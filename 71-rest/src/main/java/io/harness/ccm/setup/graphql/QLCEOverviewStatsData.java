package io.harness.ccm.setup.graphql;

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
public class QLCEOverviewStatsData {
  Boolean cloudConnectorsPresent;
  Boolean awsConnectorsPresent;
  Boolean gcpConnectorsPresent;
  Boolean applicationDataPresent;
  Boolean clusterDataPresent;
}
