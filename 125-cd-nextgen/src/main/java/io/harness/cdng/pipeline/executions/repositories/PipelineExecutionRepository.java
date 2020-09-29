package io.harness.cdng.pipeline.executions.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface PipelineExecutionRepository
    extends PagingAndSortingRepository<PipelineExecutionSummary, String>, PipelineExecutionRepositoryCustom {
  <S extends PipelineExecutionSummary> S save(S pipelineExecutionSummary);

  Optional<PipelineExecutionSummary> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String planExecutionId);

  Optional<PipelineExecutionSummary> findByPlanExecutionId(String planExecutionId);
}
