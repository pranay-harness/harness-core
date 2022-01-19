package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.archiver.nodeexecution.NodeExecutionArchive;
import org.springframework.data.repository.CrudRepository;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

@OwnedBy(PIPELINE)
@HarnessRepo
public interface NodeExecutionArchiveRepository extends CrudRepository<NodeExecutionArchive, String> {}
