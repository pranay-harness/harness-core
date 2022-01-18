package io.harness.archiver;

import io.harness.archiver.beans.PlanExecutionArchive;
import io.harness.repositories.PlanExecutionArchiveRepository;

import com.google.inject.Inject;

public class PlanExecutionArchiveServiceImpl implements PlanExecutionArchiveService {
  @Inject private PlanExecutionArchiveRepository repository;

  @Override
  public PlanExecutionArchive save(PlanExecutionArchive planExecutionArchive) {
    return repository.save(planExecutionArchive);
  }
}
