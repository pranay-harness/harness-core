package io.harness.repositories.executions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface PmsExecutionSummaryRespository
    extends PagingAndSortingRepository<PipelineExecutionSummaryEntity, String>, PmsExecutionSummaryRepositoryCustom {
  Optional<PipelineExecutionSummaryEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
      String accountId, String orgIdentifier, String projectIdentifier, String planExecutionId);

  Optional<PipelineExecutionSummaryEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String planExecutionId, boolean notPipelineDeleted);
}
