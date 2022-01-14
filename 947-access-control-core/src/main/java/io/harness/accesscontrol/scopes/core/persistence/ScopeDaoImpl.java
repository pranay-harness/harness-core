package io.harness.accesscontrol.scopes.core.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class ScopeDaoImpl implements ScopeDao {
  private final ScopeRepository scopeRepository;

  @Inject
  public ScopeDaoImpl(ScopeRepository scopeRepository) {
    this.scopeRepository = scopeRepository;
  }

  @Override
  public long saveAll(List<ScopeDBO> scopes) {
    return scopeRepository.insertAllIgnoringDuplicates(scopes);
  }

  @Override
  public ScopeDBO createIfNotPresent(ScopeDBO scope) {
    Optional<ScopeDBO> savedScope = scopeRepository.findByIdentifier(scope.getIdentifier());
    return savedScope.orElseGet(() -> scopeRepository.save(scope));
  }

  @Override
  public Optional<ScopeDBO> get(String identifier) {
    return scopeRepository.findByIdentifier(identifier);
  }

  @Override
  public Optional<ScopeDBO> delete(String identifier) {
    return scopeRepository.deleteByIdentifier(identifier).stream().findFirst();
  }
}
