package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.health.CEHealthStatus;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
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
