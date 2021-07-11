package io.harness.repositories.user.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.repositories.user.custom.UserMetadataRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserMetadataRepository
    extends PagingAndSortingRepository<UserMetadata, String>, UserMetadataRepositoryCustom {
  Optional<UserMetadata> findDistinctByUserId(String userId);

  Optional<UserMetadata> findDistinctByEmail(String email);
}
