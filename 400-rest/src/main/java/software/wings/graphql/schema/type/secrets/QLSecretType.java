package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(Module._380_CG_GRAPHQL)
public enum QLSecretType implements QLEnum {
  ENCRYPTED_TEXT,
  ENCRYPTED_FILE,
  WINRM_CREDENTIAL,
  SSH_CREDENTIAL;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
