package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateWinRMCredentialInputKeys")
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
