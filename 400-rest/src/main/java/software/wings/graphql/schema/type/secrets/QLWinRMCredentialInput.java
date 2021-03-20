package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateWinRMCredentialInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLWinRMCredentialInput {
  private String name;
  private String domain;
  private QLAuthScheme authenticationScheme;
  private String userName;
  private String passwordSecretId;
  private Boolean useSSL;
  private Boolean skipCertCheck;
  private Integer port;
  private QLUsageScope usageScope;
}
