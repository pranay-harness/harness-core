package io.harness.accesscontrol.scopes.core.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface ScopeRepository extends PagingAndSortingRepository<ScopeDBO, String>, ScopeRepositoryCustom {
  Optional<ScopeDBO> findByIdentifier(String identifier);

  List<ScopeDBO> deleteByIdentifier(String identifier);
}
