package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(PL)
public interface GitSyncErrorRepository extends CrudRepository<GitSyncError, String>, GitSyncErrorRepositoryCustom {
  GitSyncError findByAccountIdentifierAndCompleteFilePathAndErrorType(
      String accountId, String yamlFilePath, GitSyncErrorType errorType);

  // todo @bhavya: Revisit it while implementing service layer
  Long deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndCompleteFilePathIn(
      String accountId, String orgId, String projectId, List<String> yamlFilePath);
}
