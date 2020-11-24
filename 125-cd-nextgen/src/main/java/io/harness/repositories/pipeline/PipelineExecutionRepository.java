package io.harness.repositories.pipeline;

import io.harness.annotation.HarnessRepo;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PipelineExecutionRepository
    extends PagingAndSortingRepository<PipelineExecutionSummary, String>, PipelineExecutionRepositoryCustom {
  <S extends PipelineExecutionSummary> S save(S pipelineExecutionSummary);

  Optional<PipelineExecutionSummary> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String planExecutionId);

  Optional<PipelineExecutionSummary> findByPlanExecutionId(String planExecutionId);
}
