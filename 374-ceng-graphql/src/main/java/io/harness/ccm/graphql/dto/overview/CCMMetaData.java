/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.overview;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CCMMetaData {
  @GraphQLNonNull @Builder.Default Boolean k8sClusterConnectorPresent = false;
  @GraphQLNonNull @Builder.Default Boolean cloudDataPresent = false;
  @GraphQLNonNull @Builder.Default Boolean awsConnectorsPresent = false;
  @GraphQLNonNull @Builder.Default Boolean gcpConnectorsPresent = false;
  @GraphQLNonNull @Builder.Default Boolean azureConnectorsPresent = false;
  @GraphQLNonNull @Builder.Default Boolean applicationDataPresent = false;
  @GraphQLNonNull @Builder.Default Boolean inventoryDataPresent = false;
  @GraphQLNonNull @Builder.Default Boolean clusterDataPresent = false;
  @GraphQLNonNull @Builder.Default Boolean isSampleClusterPresent = false;
  String defaultAzurePerspectiveId;
  String defaultAwsPerspectiveId;
  String defaultGcpPerspectiveId;
  String defaultClusterPerspectiveId;
}
