package io.harness.secrets;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretScopeMetadata;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.UsageRestrictions;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

public interface SecretsRBACService {
  boolean hasAccessToEditSecret(@NotEmpty String accountId, @NotNull SecretScopeMetadata secretScopeMetadata);

  boolean hasAccessToEditSecrets(@NotEmpty String accountId, @NotNull Set<SecretScopeMetadata> secretScopeMetadata);

  boolean hasAccessToReadSecret(
      @NotEmpty String accountId, @NotNull SecretScopeMetadata secretScopeMetadata, String appId, String envId);

  boolean hasAccessToReadSecrets(
      @NotEmpty String accountId, @NotNull Set<SecretScopeMetadata> secretsScopeMetadata, String appId, String envId);

  boolean hasAccessToAccountScopedSecrets(@NotEmpty String accountId);

  List<SecretScopeMetadata> filterSecretsByReadPermission(@NotEmpty String accountId,
      @NotNull List<SecretScopeMetadata> secretScopeMetadataList, String appId, String envId);

  void canReplacePermissions(@NotEmpty String accountId, @NotNull SecretScopeMetadata newSecretScopeMetadata,
      @NotNull SecretScopeMetadata oldSecretScopeMetadata, boolean validateReferences);

  void canSetPermissions(@NotEmpty String accountId, @NotNull SecretScopeMetadata secretScopeMetadata);

  UsageRestrictions getDefaultUsageRestrictions(@NotEmpty String accountId);

  boolean isScopeInConflict(EncryptedData encryptedData, SecretManagerConfig secretManagerConfig);
}
