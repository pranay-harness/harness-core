package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.pms.pipeline.PipelineEntity;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@GitSyncableHarnessRepo
@Transactional
@OwnedBy(PIPELINE)
public interface PMSPipelineRepository extends Repository<PipelineEntity, String>, PMSPipelineRepositoryCustom {}
