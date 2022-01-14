package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface ServiceAccountRepository
    extends PagingAndSortingRepository<ServiceAccountDBO, String>, ServiceAccountCustomRepository {
  Optional<ServiceAccountDBO> findByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);

  Page<ServiceAccountDBO> findByScopeIdentifier(String scopeIdentifier, Pageable pageable);

  List<ServiceAccountDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
}
