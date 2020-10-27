package io.harness.secrets;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface SecretsRBACService {
  boolean hasAccessToEditSecret(@NotEmpty String accountId, @Valid @NotNull ScopedEntity scopedEntity);

  boolean hasAccessToReadSecret(@NotEmpty String accountId, @Valid @NotNull ScopedEntity scopedEntity);

  boolean hasAccessToReadSecret(@NotEmpty String accountId, @Valid @NotNull ScopedEntity scopedEntity,
      @NotEmpty String appId, @NotEmpty String envId);

  boolean hasAccessToReadSecrets(String accountId, List<ScopedEntity> scopedEntities, String appId, String envId);

  void canReplacePermissions(@NotEmpty String accountId, @NotNull ScopedEntity newScopedEntity,
      @NotNull ScopedEntity oldScopedEntity, @NotEmpty String secretId, boolean validateReferences);

  void canSetPermissions(@NotEmpty String accountId, @NotNull ScopedEntity scopedEntity);

  UsageRestrictions getDefaultUsageRestrictions(String accountId);
}