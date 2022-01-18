package io.harness.repositories;

import io.harness.archiver.beans.NodeExecutionArchive;
import io.harness.archiver.beans.PlanExecutionArchive;
import org.springframework.data.repository.CrudRepository;

public interface PlanExecutionArchiveRepository extends CrudRepository<PlanExecutionArchive, String> {}
