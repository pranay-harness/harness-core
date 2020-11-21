package io.harness.ccm.setup.graphql;

import io.harness.ccm.health.CEHealthStatus;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEConnector implements QLObject {
  private String settingId;
  private String accountName;
  private String s3BucketName;
  private String curReportName;
  private String crossAccountRoleArn;
  private CEHealthStatus ceHealthStatus;
  private QLInfraTypesEnum infraType;
}
