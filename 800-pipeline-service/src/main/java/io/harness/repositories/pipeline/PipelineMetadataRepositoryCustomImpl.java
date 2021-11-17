package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineMetadata;
import io.harness.pms.pipeline.PipelineMetadata.PipelineMetadataKeys;

import com.google.inject.Inject;
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

  @Override
  public PipelineMetadata incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId) {
    Update update = new Update();
    update.inc(PipelineMetadataKeys.runSequence);
    Criteria criteria = Criteria.where(PipelineMetadataKeys.accountId)
                            .is(accountId)
                            .and(PipelineMetadataKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineMetadataKeys.pipelineIdentifier)
                            .is(pipelineId);
    PipelineMetadata pipelineMetadata = mongoTemplate.findAndModify(
        new Query(criteria), update, new FindAndModifyOptions().returnNew(true), PipelineMetadata.class);
    return pipelineMetadata;
  }

  @Override
  public long getRunSequence(String accountId, String orgId, String projectIdentifier, String pipelineId,
      ExecutionSummaryInfo executionSummaryInfo) {
    Update update = new Update();
    update.set(PipelineMetadata.PipelineMetadataKeys.executionSummaryInfo, executionSummaryInfo);
    Criteria criteria = Criteria.where(PipelineMetadataKeys.accountId)
                            .is(accountId)
                            .and(PipelineMetadataKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineMetadataKeys.pipelineIdentifier)
                            .is(pipelineId);
    PipelineMetadata pipelineMetadata = mongoTemplate.findAndModify(
        new Query(criteria), update, new FindAndModifyOptions().returnNew(true), PipelineMetadata.class);
    if (pipelineMetadata != null) {
      return pipelineMetadata.getRunSequence();
    }
    return 0;
  }
}