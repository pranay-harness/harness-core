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
import io.harness.ccm.health.CEHealthStatus;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class QLCEConnector implements QLObject {
  private String settingId;
  private String accountName;
  private String s3BucketName;
  private String curReportName;
  private String crossAccountRoleArn;
  private CEHealthStatus ceHealthStatus;
  private String azureStorageAccountName;
  private String azureStorageContainerName;
  private String azureStorageDirectoryName;
  private String azureSubscriptionId;
  private String azureTenantId;
  private QLInfraTypesEnum infraType;
}
