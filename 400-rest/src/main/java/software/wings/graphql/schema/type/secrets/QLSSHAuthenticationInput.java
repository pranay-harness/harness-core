package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHAuthenticationInputKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSSHAuthenticationInput implements QLObject {
  String userName;
  Integer port;
  QLSSHAuthenticationMethod sshAuthenticationMethod;
}
