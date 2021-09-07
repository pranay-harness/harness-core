package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.template.entity.TemplateEntity;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@GitSyncableHarnessRepo
@Transactional
@OwnedBy(CDC)
public interface NGTemplateRepository extends Repository<TemplateEntity, String>, NGTemplateRepositoryCustom {}
