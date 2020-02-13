package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialsKeys")
public class QLWinRMCredential implements QLSecret {
  private String id;
  private String name;
  private QLAuthScheme authenticationScheme;
  private String userName;
  private Boolean useSSL;
  private String domain;
  private Boolean skipCertCheck;
  private Integer port;
}
