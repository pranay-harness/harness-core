package io.harness.gitsync.gitsyncerror.dao.api.repositories.gitSyncError;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;
import io.harness.ng.core.gitsync.ChangeType;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

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
