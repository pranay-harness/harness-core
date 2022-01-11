/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretScopeMetadata;

import software.wings.security.UsageRestrictions;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

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
