/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagers;

import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface SecretsManagerRBACService {
  boolean hasAccessToEditSM(@NotEmpty String accountId, @NotNull ScopedEntity scopedEntity);

  boolean hasAccessToReadSM(
      @NotEmpty String accountId, @NotNull ScopedEntity scopedEntity, @NotEmpty String appId, @NotEmpty String envId);

  void canChangePermissions(
      @NotEmpty String accountId, @NotNull ScopedEntity newScopedEntity, @NotNull ScopedEntity oldScopedEntity);

  void canSetPermissions(@NotEmpty String accountId, @NotNull ScopedEntity scopedEntity);

  boolean areUsageScopesSubset(
      @NotEmpty String accountId, @NotNull ScopedEntity scopedEntity, @NotNull ScopedEntity parentScopedEntity);

  UsageRestrictions getMaximalAllowedScopes(@NotEmpty String accountId, @NotNull ScopedEntity scopedEntity);
}
