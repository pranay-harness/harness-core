package io.harness.archiver.planexecution;

import com.google.inject.Inject;
import io.harness.repositories.PlanExecutionArchiveRepository;

public class PlanExecutionArchiveServiceImpl implements PlanExecutionArchiveService {
  @Inject private PlanExecutionArchiveRepository repository;

  @Override
  public PlanExecutionArchive save(PlanExecutionArchive planExecutionArchive) {
    return repository.save(planExecutionArchive);
  }
}
