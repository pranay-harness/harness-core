package io.harness.repositories.executions;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PmsExecutionSummaryRespository
    extends PagingAndSortingRepository<PipelineExecutionSummaryEntity, String>, PmsExecutionSummaryRepositoryCustom {
  Optional<PipelineExecutionSummaryEntity> findByPlanExecutionId(String planExecutionId);
}
