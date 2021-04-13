package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(DX)
public interface GitBranchService {
  List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String repoURL);

  List<String> listBranchesForRepoByGitSyncConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier);
}
