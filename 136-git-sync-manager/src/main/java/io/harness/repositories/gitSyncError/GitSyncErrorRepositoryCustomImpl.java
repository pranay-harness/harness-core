package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType.GIT_TO_HARNESS;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.jooq.tools.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PL)
public class GitSyncErrorRepositoryCustomImpl implements GitSyncErrorRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass) {
    return mongoTemplate.aggregate(aggregation, GitSyncError.class, castClass);
  }

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitSyncErrorKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitSyncError.class);
  }

  @Override
  public UpdateResult upsertGitError(String accountId, String yamlFilePath, String errorMessage, ChangeType changeType,
      GitSyncErrorDetails gitSyncErrorDetails, String gitConnector, String repo, String branchName,
      String yamlGitConfigId) {
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountId)
                            .and(GitSyncErrorKeys.repoUrl)
                            .is(repo)
                            .and(GitSyncErrorKeys.branchName)
                            .is(branchName)
                            .and(GitSyncErrorKeys.completeFilePath)
                            .is(yamlFilePath)
                            .and(GitSyncErrorKeys.errorType)
                            .is(GIT_TO_HARNESS);
    // todo @Deepak: Revisit this file while creating the git error service
    Update update = update(GitSyncErrorKeys.repoUrl, repo).set(GitSyncErrorKeys.branchName, branchName);
    update.setOnInsert(GitSyncErrorKeys.uuid, generateUuid())
        .set(GitSyncErrorKeys.accountIdentifier, accountId)
        .set(GitSyncErrorKeys.errorType, GIT_TO_HARNESS)
        .set(GitSyncErrorKeys.completeFilePath, yamlFilePath)
        .set(GitSyncErrorKeys.failureReason,
            StringUtils.isEmpty(errorMessage) ? "Reason could not be captured." : errorMessage)
        .set(GitSyncErrorKeys.additionalErrorDetails, gitSyncErrorDetails)
        .set(GitSyncErrorKeys.changeType, changeType);

    return mongoTemplate.upsert(query(criteria), update, GitSyncError.class);
  }
}
