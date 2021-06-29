package io.harness.repositories.ng.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Token;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface TokenRepository extends PagingAndSortingRepository<Token, String> {
  long deleteByIdentifier(String identifier);
  Optional<Token> findByIdentifier(String identifier);
}
