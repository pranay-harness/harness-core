package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretUpdateData;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SecretsDao {
  String ID_KEY = "_id";

  Optional<EncryptedData> getSecretById(@NotEmpty String accountId, @NotEmpty String secretId);

  Optional<EncryptedData> getAccountScopedSecretById(@NotEmpty String accountId, @NotEmpty String secretId);

  Optional<EncryptedData> getSecretByName(@NotEmpty String accountId, @NotEmpty String secretName);

  Optional<EncryptedData> getAccountScopedSecretByName(@NotEmpty String accountId, @NotEmpty String secretName);

  Optional<EncryptedData> getSecretByKeyOrPath(
      @NotEmpty String accountId, @NotEmpty EncryptionType encryptionType, String key, String path);

  String saveSecret(@Valid @NotNull EncryptedData encryptedData);

  EncryptedData updateSecret(@Valid @NotNull SecretUpdateData secretUpdateData, EncryptedRecord updatedEncryptedData);

  EncryptedData migrateSecret(
      @NotEmpty String accountId, @NotEmpty String secretId, @Valid @NotNull EncryptedRecord encryptedRecord);

  boolean deleteSecret(@NotEmpty String accountId, @NotEmpty String secretId);

  MorphiaIterator<EncryptedData, EncryptedData> listSecretsBySecretManager(
      @NotEmpty String accountId, @NotEmpty String secretManagerId, boolean shouldIncludeSecretManagerSecrets);
}