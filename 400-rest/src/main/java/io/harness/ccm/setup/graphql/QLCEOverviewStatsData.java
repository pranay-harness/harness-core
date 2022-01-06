/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QLCEOverviewStatsData {
  Boolean ceEnabledClusterPresent;
  Boolean cloudConnectorsPresent;
  Boolean awsConnectorsPresent;
  Boolean gcpConnectorsPresent;
  Boolean azureConnectorsPresent;
  Boolean applicationDataPresent;
  Boolean inventoryDataPresent;
  Boolean clusterDataPresent;
  String defaultAzurePerspectiveId;
  String defaultAwsPerspectiveId;
  String defaultGcpPerspectiveId;
  String defaultClusterPerspectiveId;
  @Builder.Default Boolean isSampleClusterPresent = false;
}
