package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHAuthenticationKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSSHAuthentication implements QLSSHAuthenticationType {
  String userName;
  Integer port;
}
