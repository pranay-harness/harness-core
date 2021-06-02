package io.harness.repositories.gitSyncError;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitSyncErrorRepository
    extends PagingAndSortingRepository<GitSyncError, String>, GitSyncErrorRepositoryCustom {
  GitSyncError findByAccountIdAndYamlFilePathAndGitSyncDirection(
      String accountId, String yamlFilePath, GitSyncDirection direction);

  Long removeByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePathIn(
      String accountId, String orgId, String projectId, List<String> yamlFilePath);
}
