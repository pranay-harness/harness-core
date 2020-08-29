package io.harness.gitsync.gitsyncerror.dao.api.repositories.gitSyncError;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitSyncErrorRepository
    extends PagingAndSortingRepository<GitSyncError, String>, GitSyncErrorRepositoryCustom {
  Long removeByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePathIn(
      String accountId, String orgId, String projectId, List<String> yamlFilePath);
}
