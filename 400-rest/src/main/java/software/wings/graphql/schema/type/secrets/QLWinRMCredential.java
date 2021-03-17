package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialsKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLWinRMCredential implements QLSecret {
  private String id;
  private String name;
  private QLSecretType secretType;
  private QLAuthScheme authenticationScheme;
  private String userName;
  private Boolean useSSL;
  private String domain;
  private Boolean skipCertCheck;
  private Integer port;
  private QLUsageScope usageScope;
}
