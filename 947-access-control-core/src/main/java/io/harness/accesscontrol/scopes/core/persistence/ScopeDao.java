package io.harness.accesscontrol.scopes.core.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface ScopeDao {
  long saveAll(@NotNull @Valid List<ScopeDBO> scopes);

  ScopeDBO createIfNotPresent(@NotNull @Valid ScopeDBO scope);

  Optional<ScopeDBO> get(@NotEmpty String identifier);

  Optional<ScopeDBO> delete(@NotEmpty String identifier);
}
