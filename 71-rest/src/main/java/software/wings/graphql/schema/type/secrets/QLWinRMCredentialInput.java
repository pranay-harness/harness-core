package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateWinRMCredentialInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLWinRMCredentialInput {
  private String name;
  private String domain;
  private QLAuthScheme authenticationScheme;
  private String userName;
  private String password;
  private boolean useSSL;
  private boolean skipCertCheck;
  private int port;
  private String clientMutationId;
}
