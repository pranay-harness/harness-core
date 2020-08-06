package io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivity;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

public interface GitFileActivityRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);

  UpdateResult updateGitFileActivityStatus(GitFileActivity.Status status, String errorMsg, String accountId,
      String commitId, List<String> filePaths, GitFileActivity.Status oldStatus);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);
}
