package io.harness.accesscontrol.scopes.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface ScopeService {
  long saveAll(@NotNull @Valid List<Scope> scopes);
  Scope getOrCreate(@NotNull @Valid Scope scope);
  boolean isPresent(@NotEmpty String identifier);
  Optional<Scope> deleteIfPresent(@NotEmpty String identifier);
  Scope buildScopeFromScopeIdentifier(@NotNull String identifier);

  boolean areScopeLevelsValid(@NotNull Set<String> scopeLevels);
  Set<String> getAllScopeLevels();
}
