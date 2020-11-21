package io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivitySummary;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitFileActivitySummaryRepository
    extends CrudRepository<GitFileActivitySummary, String>, GitFileActivitySummaryRepositoryCustom {}
