package io.harness.repositories.gitSyncError;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitSyncErrorRepository
    extends PagingAndSortingRepository<GitSyncError, String>, GitSyncErrorRepositoryCustom {
  GitSyncError findByAccountIdentifierAndCompleteFilePathAndErrorType(
      String accountId, String yamlFilePath, GitSyncErrorType errorType);

  Long removeByAccountIdentifierAndCompleteFilePathIn(String accountId, List<String> yamlFilePath);
}
