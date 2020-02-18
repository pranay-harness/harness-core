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
  private String password;
  private Boolean useSSL;
  private Boolean skipCertCheck;
  private Integer port;
  private String clientMutationId;
  private QLUsageScope usageScope;
}
