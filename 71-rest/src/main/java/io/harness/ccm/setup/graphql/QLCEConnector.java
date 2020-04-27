package io.harness.ccm.setup.graphql;

import io.harness.ccm.health.CEHealthStatus;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

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
