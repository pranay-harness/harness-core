package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialsKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLWinRMCredential implements QLSecret {
  private String id;
  private String name;
  private QLAuthScheme authenticationScheme;
  private String username;
  private Boolean useSSL;
  private String domain;
  private Boolean skipCertCheck;
  private int port;
}
