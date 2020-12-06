package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLEnum;

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
