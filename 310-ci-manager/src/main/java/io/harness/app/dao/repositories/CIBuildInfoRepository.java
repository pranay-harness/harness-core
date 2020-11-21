package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ci.beans.entities.CIBuild;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface CIBuildInfoRepository
    extends PagingAndSortingRepository<CIBuild, String>, CIBuildInfoRepositoryCustom {
  Optional<CIBuild> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndUuid(
      String accountId, String organizationId, String projectId, String identifier);
}
