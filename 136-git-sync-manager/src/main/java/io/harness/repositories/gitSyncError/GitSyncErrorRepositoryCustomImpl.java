package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType.GIT_TO_HARNESS;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;

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

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitSyncErrorRepositoryCustomImpl implements GitSyncErrorRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass) {
    return mongoTemplate.aggregate(aggregation, YamlChangeSet.class, castClass);
  }

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitSyncErrorKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitSyncError.class);
  }

  @Override
  public UpdateResult upsertGitError(String accountId, String yamlFilePath, GitSyncDirection gitSyncDirection,
      String errorMessage, boolean fullSyncPath, ChangeType changeType, GitSyncErrorDetails gitSyncErrorDetails,
      String gitConnector, String repo, String branchName, String rootFolder, String yamlGitConfigId, String projectId,
      String orgId) {
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountId)
                            .and(GitSyncErrorKeys.completeFilePath)
                            .is(yamlFilePath)
                            .and(GitSyncErrorKeys.errorType)
                            .is(GIT_TO_HARNESS);
    // todo @Deepak: Revisit this file while creating the git error service
    /* Update update = update(GitSyncErrorKeys.yamlGitConfigRef, gitConnector)
                         .set(GitSyncErrorKeys.branchName, branchName)
                         .set(GitSyncErrorKeys.repoURL, repo)
                         .set(GitSyncErrorKeys.rootFolder, rootFolder)
                         .set(GitSyncErrorKeys.fullSyncPath, fullSyncPath)
                         .set(GitSyncErrorKeys.yamlGitConfigRef, yamlGitConfigId);
     update.setOnInsert(GitSyncErrorKeys.uuid, generateUuid())
         .set(GitSyncErrorKeys.accountIdentifier, accountId)
         .set(GitSyncErrorKeys.errorType, GIT_TO_HARNESS)
         .set(GitSyncErrorKeys.completeFilePath, yamlFilePath)
         .set(GitSyncErrorKeys.failureReason, errorMessage != null ? errorMessage : "Reason could not be captured.")
         .set(GitSyncErrorKeys.projectIdentifier, projectId)
         .set(GitSyncErrorKeys.orgIdentifier, orgId)
         .set(GitSyncErrorKeys.additionalErrorDetails, gitSyncErrorDetails)
         .set(GitSyncErrorKeys.changeType, changeType);

     return mongoTemplate.upsert(query(criteria), update, GitSyncError.class);*/
    return null;
  }

  @Override
  public List<GitSyncError> getActiveGitSyncError(String accountId, long fromTimestamp,
      GitSyncDirection gitSyncDirection, String gitConnectorId, String repo, String branchName, String rootFolder) {
    // todo @Deepak: Revisit this file while creating the git error service
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountId)
                            .and(GitSyncErrorKeys.createdAt)
                            .gt(fromTimestamp)
                            .and(GitSyncErrorKeys.errorType)
                            .is(GIT_TO_HARNESS)
                            .and(GitSyncErrorKeys.status)
                            .is(GitSyncErrorStatus.ACTIVE)
                            .and(GitSyncErrorKeys.branchName)
                            .is(branchName)
                            .and(GitSyncErrorKeys.repoUrl)
                            .is(repo);
    /*if (rootFolder != null) {
      criteria.and(GitSyncErrorKeys.rootFolder).is(rootFolder);
    }*/

    return mongoTemplate.find(query(criteria), GitSyncError.class);
  }
}
