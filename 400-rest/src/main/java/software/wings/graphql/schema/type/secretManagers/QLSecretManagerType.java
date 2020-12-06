package software.wings.graphql.schema.type.secretManagers;

import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.VAULT;

import software.wings.graphql.schema.type.QLEnum;

public enum QLSecretManagerType implements QLEnum {
  AWS_KMS,
  AWS_SECRET_MANAGER,
  HASHICORP_VAULT,
  AZURE_KEY_VAULT,
  CYBERARK,
  GOOGLE_KMS,
  CUSTOM;

  // Since entity name are different in DB we have to translate the GraphQL entity Name.
  private String getEntityType(String value) {
    if (AWS_KMS.name().equals(value)) {
      return KMS.name();
    } else if (HASHICORP_VAULT.name().equals(value)) {
      return VAULT.name();
    } else if (AZURE_KEY_VAULT.name().equals(value)) {
      return AZURE_VAULT.name();
    } else if (GOOGLE_KMS.name().equals(value)) {
      return GCP_KMS.name();
    }
    return value;
  }

  @Override
  public String getStringValue() {
    return getEntityType(this.name());
  }
}
