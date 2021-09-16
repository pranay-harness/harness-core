package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@OwnedBy(PL)
public interface GitSyncErrorRepositoryCustom {
  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);

  DeleteResult deleteByIds(List<String> ids);

  UpdateResult upsertGitError(String accountId, String yamlFilePath, String errorMessage, ChangeType changeType,
      GitSyncErrorDetails gitSyncErrorDetails, String gitConnector, String repo, String branchName,
      String yamlGitConfigId);
}
