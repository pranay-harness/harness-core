package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PL)
public class GitSyncErrorRepositoryCustomImpl implements GitSyncErrorRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass) {
    return mongoTemplate.aggregate(aggregation, "gitSyncErrorNG", castClass);
  }

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitSyncErrorKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitSyncError.class);
  }

  @Override
  public UpdateResult upsertGitError(Criteria criteria, Update update) {
    return mongoTemplate.upsert(query(criteria), update, GitSyncError.class);
  }

  @Override
  public Page<GitSyncError> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<GitSyncError> gitSyncErrors = mongoTemplate.find(query, GitSyncError.class);
    return PageableExecutionUtils.getPage(
        gitSyncErrors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GitSyncError.class));
  }

  @Override
  public long count(Criteria criteria) {
    return mongoTemplate.count(query(criteria), GitSyncError.class);
  }
}
