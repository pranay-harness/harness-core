package software.wings.graphql.schema.type.secretManagers;

import static io.harness.security.encryption.EncryptionType.AWS_SECRETS_MANAGER;
import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.security.encryption.EncryptionType;

import software.wings.graphql.schema.type.QLEnum;

import lombok.Getter;

public enum QLSecretManagerType implements QLEnum {
  AWS_KMS(KMS),
  AWS_SECRET_MANAGER(AWS_SECRETS_MANAGER),
  HASHICORP_VAULT(VAULT),
  AZURE_KEY_VAULT(AZURE_VAULT),
  CYBERARK(EncryptionType.CYBERARK),
  GOOGLE_KMS(GCP_KMS),
  CUSTOM(EncryptionType.CUSTOM);

  @Getter private final EncryptionType encryptionType;

  QLSecretManagerType(EncryptionType encryptionType) {
    this.encryptionType = encryptionType;
  }

  @Override
  public String getStringValue() {
    return this.getEncryptionType().name();
  }
}
