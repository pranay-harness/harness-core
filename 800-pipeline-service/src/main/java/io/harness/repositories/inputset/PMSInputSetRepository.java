package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@GitSyncableHarnessRepo
@Transactional
@OwnedBy(PIPELINE)
public interface PMSInputSetRepository extends Repository<InputSetEntity, String>, PMSInputSetRepositoryCustom {}
