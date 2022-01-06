package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineMetadata;
import io.harness.pms.pipeline.PipelineMetadata.PipelineMetadataKeys;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PipelineMetadataRepositoryCustomImpl implements PipelineMetadataRepositoryCustom {
  MongoTemplate mongoTemplate;
  GitAwarePersistence gitAwarePersistence;

  @Override
  public PipelineMetadata incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId) {
    Update update = new Update();
    Criteria criteria = Criteria.where(PipelineMetadataKeys.accountIdentifier)
                            .is(accountId)
                            .and(PipelineMetadataKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineMetadataKeys.identifier)
                            .is(pipelineId);
    Criteria gitSyncCriteria =
        gitAwarePersistence.getCriteriaWithGitSync(projectIdentifier, orgId, accountId, PipelineMetadata.class);
    if (gitSyncCriteria != null) {
      criteria = new Criteria().andOperator(criteria, gitSyncCriteria);
    }

    update.inc(PipelineMetadataKeys.runSequence);
    return mongoTemplate.findAndModify(
        new Query(criteria), update, new FindAndModifyOptions().returnNew(true), PipelineMetadata.class);
  }

  @Override
  public long getRunSequence(String accountId, String orgId, String projectIdentifier, String pipelineId,
      ExecutionSummaryInfo executionSummaryInfo) {
    Update update = new Update();
    update.set(PipelineMetadataKeys.executionSummaryInfo, executionSummaryInfo);
    Criteria criteria = Criteria.where(PipelineMetadataKeys.accountIdentifier)
                            .is(accountId)
                            .and(PipelineMetadataKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineMetadataKeys.identifier)
                            .is(pipelineId);
    PipelineMetadata pipelineMetadata = mongoTemplate.findAndModify(
        new Query(criteria), update, new FindAndModifyOptions().returnNew(true), PipelineMetadata.class);
    if (pipelineMetadata != null) {
      return pipelineMetadata.getRunSequence();
    }
    return 0;
  }

  @Override
  public Optional<PipelineMetadata> getPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier) {
    Criteria criteria = Criteria.where(PipelineMetadataKeys.accountIdentifier)
                            .is(accountId)
                            .and(PipelineMetadataKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineMetadataKeys.identifier)
                            .is(identifier);
    Criteria gitSyncCriteria =
        gitAwarePersistence.getCriteriaWithGitSync(projectIdentifier, orgId, accountId, PipelineMetadata.class);
    if (gitSyncCriteria != null) {
      criteria = new Criteria().andOperator(criteria, gitSyncCriteria);
    }
    return Optional.ofNullable(mongoTemplate.findOne(new Query(criteria), PipelineMetadata.class));
  }
}