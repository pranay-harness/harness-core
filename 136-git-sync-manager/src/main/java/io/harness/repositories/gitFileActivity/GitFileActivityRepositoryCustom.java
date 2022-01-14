package io.harness.repositories.gitFileActivity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@OwnedBy(DX)
public interface GitFileActivityRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);

  UpdateResult updateGitFileActivityStatus(GitFileActivity.Status status, String errorMsg, String accountId,
      String commitId, List<String> filePaths, GitFileActivity.Status oldStatus);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);
}
