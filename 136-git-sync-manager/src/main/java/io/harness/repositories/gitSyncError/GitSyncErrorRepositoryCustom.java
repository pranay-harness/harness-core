package io.harness.repositories.gitSyncError;

import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

public interface GitSyncErrorRepositoryCustom {
  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);

  DeleteResult deleteByIds(List<String> ids);

  UpdateResult upsertGitError(String accountId, String yamlFilePath, GitSyncError.GitSyncDirection gitSyncDirection,
      String errorMessage, boolean fullSyncPath, ChangeType changeType, GitSyncErrorDetails gitSyncErrorDetails,
      String gitConnector, String repo, String branchName, String rootFolder, String yamlGitConfigId, String projectId,
      String orgId);

  List<GitSyncError> getActiveGitSyncError(String accountId, long fromTimestamp,
      GitSyncError.GitSyncDirection gitSyncDirection, String gitConnectorId, String repo, String branchName,
      String rootFolder);
}
