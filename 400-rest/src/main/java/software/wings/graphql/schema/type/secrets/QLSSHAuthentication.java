package software.wings.graphql.schema.type.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHAuthenticationKeys")
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSSHAuthentication implements QLSSHAuthenticationType {
  String userName;
  Integer port;
  QLSSHAuthenticationMethodOutput sshAuthenticationMethod;
}
