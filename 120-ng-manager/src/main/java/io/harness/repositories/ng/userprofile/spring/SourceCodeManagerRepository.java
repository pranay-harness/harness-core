package io.harness.repositories.ng.userprofile.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.userprofile.entities.SourceCodeManager;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface SourceCodeManagerRepository extends PagingAndSortingRepository<SourceCodeManager, String> {
  List<SourceCodeManager> findByUserIdentifier(String userIdentifier);
}
