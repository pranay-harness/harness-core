package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ci.beans.entities.CIBuild;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface CIBuildInfoRepository
    extends PagingAndSortingRepository<CIBuild, String>, CIBuildInfoRepositoryCustom {
  Optional<CIBuild> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndUuid(
      String accountId, String organizationId, String projectId, String identifier);
}
