package io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivity;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.GitFileActivityKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))

public class GitFileActivityRepositoryCustomImpl implements GitFileActivityRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitFileActivityKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitFileActivity.class);
  }

  @Override
  public UpdateResult updateGitFileActivityStatus(GitFileActivity.Status status, String errorMsg, String accountId,
      String commitId, List<String> filePaths, GitFileActivity.Status oldStatus) {
    Update update = update(GitFileActivityKeys.status, status);
    if (errorMsg != null) {
      update.set(GitFileActivityKeys.errorMessage, errorMsg);
    }

    Criteria criteria = Criteria.where(GitFileActivityKeys.accountId)
                            .is(accountId)
                            .and(GitFileActivityKeys.processingCommitId)
                            .is(commitId);
    if (oldStatus != null) {
      criteria.and(GitFileActivityKeys.status).is(oldStatus);
    }
    if (filePaths != null) {
      criteria.and(GitFileActivityKeys.filePath).in(filePaths);
    }

    Query query = query(criteria);
    return mongoTemplate.updateMulti(query, update, GitFileActivity.class);
  }

  @Override
  public <C> AggregationResults<C> aggregate(Aggregation aggregation, Class<C> castClass) {
    return mongoTemplate.aggregate(aggregation, GitFileActivity.class, castClass);
  }
}
