package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.CIPipeline;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface CIPipelineRepository
    extends PagingAndSortingRepository<CIPipeline, String>, CIPipelineRepositoryCustom {
  Optional<CIPipeline> findByAccountIdAndOrganizationIdAndProjectIdAndIdentifier(
      String accountId, String organizationId, String projectId, String identifier);
}
