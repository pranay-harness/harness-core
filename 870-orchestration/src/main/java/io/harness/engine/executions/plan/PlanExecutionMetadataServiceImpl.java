package io.harness.engine.executions.plan;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.repositories.PlanExecutionMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PlanExecutionMetadataServiceImpl implements PlanExecutionMetadataService {
  private final PlanExecutionMetadataRepository planExecutionMetadataRepository;

  @Inject
  public PlanExecutionMetadataServiceImpl(PlanExecutionMetadataRepository planExecutionMetadataRepository) {
    this.planExecutionMetadataRepository = planExecutionMetadataRepository;
  }

  @Override
  public Optional<PlanExecutionMetadata> findByPlanExecutionId(String planExecutionId) {
    return planExecutionMetadataRepository.findByPlanExecutionId(planExecutionId);
  }

  @Override
  public PlanExecutionMetadata save(PlanExecutionMetadata planExecutionMetadata) {
    return planExecutionMetadataRepository.save(planExecutionMetadata);
  }

  @Override
  public String getYamlFromPlanExecutionId(String planExecutionId) {
    Optional<PlanExecutionMetadata> planExecutionMetadata = findByPlanExecutionId(planExecutionId);

    if (!planExecutionMetadata.isPresent() || isEmpty(planExecutionMetadata.get().getYaml())) {
      return null;
    }

    return planExecutionMetadata.get().getYaml();
  }
}
