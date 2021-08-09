package io.harness.accesscontrol.preference.persistence.repositories;

import io.harness.accesscontrol.preference.persistence.models.AccessControlPreference;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PL)
public interface AccessControlPreferenceRepository extends PagingAndSortingRepository<AccessControlPreference, String> {
  Optional<AccessControlPreference> findByAccountId(String accountId);
}
