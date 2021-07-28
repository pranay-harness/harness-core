package io.harness.accesscontrol.support.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface SupportPreferenceRepository extends PagingAndSortingRepository<SupportPreferenceDBO, String> {
  Optional<SupportPreferenceDBO> findByAccountIdentifier(String accountIdentifier);
}
