package io.harness.gitsync.core.impl;

import com.google.inject.Inject;

import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.dao.api.repositories.GitCommit.GitCommitRepository;
import io.harness.gitsync.core.service.GitCommitService;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitCommitServiceImpl implements GitCommitService {
  private GitCommitRepository gitCommitRepository;

  @Override
  public GitCommit save(GitCommit gitCommit) {
    return gitCommitRepository.save(gitCommit);
  }
}
