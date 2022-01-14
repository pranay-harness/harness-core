package io.harness.repositories.ng.userprofile.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.userprofile.entities.SourceCodeManager;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@OwnedBy(PL)
@HarnessRepo
@Transactional
public interface SourceCodeManagerRepository extends PagingAndSortingRepository<SourceCodeManager, String> {
  List<SourceCodeManager> findByUserIdentifierAndAccountIdentifier(String userIdentifier, String accountIdentifier);
  long deleteByUserIdentifierAndNameAndAccountIdentifier(String userIdentifier, String name, String accountIdentifier);
}
