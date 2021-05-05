package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.springdata.SpringDataMongoUtils;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@GitSyncableHarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineRepositoryCustomImpl implements PMSPipelineRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final GitAwarePersistence gitAwarePersistence;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);

  @Override
  public PipelineEntity update(Criteria criteria, PipelineEntity pipelineEntity) {
    return update(criteria, PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity));
  }

  public PipelineEntity update(Criteria criteria, Update update) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Pipeline; attempt: {}", "[Failed]: Failed updating Pipeline; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), PipelineEntity.class));
  }
  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    int MAX_ATTEMPTS = 3;
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }

  @Override
  public UpdateResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    Update updateOperationsForDelete = PMSPipelineFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Pipeline; attempt: {}", "[Failed]: Failed deleting Pipeline; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(() -> mongoTemplate.updateFirst(query, updateOperationsForDelete, PipelineEntity.class));
  }

  @Override
  public Optional<PipelineEntity> incrementRunSequence(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean deleted) {
    Query query = query(where(PipelineEntityKeys.accountId).is(accountId))
                      .addCriteria(where(PipelineEntityKeys.orgIdentifier).is(orgIdentifier))
                      .addCriteria(where(PipelineEntityKeys.projectIdentifier).is(projectIdentifier))
                      .addCriteria(where(PipelineEntityKeys.identifier).is(pipelineIdentifier))
                      .addCriteria(where(PipelineEntityKeys.deleted).is(deleted));
    Update update = new Update();
    update.inc(PipelineEntityKeys.runSequence);
    PipelineEntity pipelineEntity =
        mongoTemplate.findAndModify(query, update, SpringDataMongoUtils.returnNewOptions, PipelineEntity.class);
    return Optional.ofNullable(pipelineEntity);
  }

  @Override
  public Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<PipelineEntity> projects = mongoTemplate.find(query, PipelineEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), PipelineEntity.class));
  }

  @Override
  public PipelineEntity save(PipelineEntity pipelineToSave, PipelineConfig yamlDTO) {
    return gitAwarePersistence.save(pipelineToSave, yamlDTO, ChangeType.ADD, PipelineEntity.class);
  }

  @Override
  public Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(PipelineEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(PipelineEntityKeys.identifier)
                                           .is(pipelineIdentifier)
                                           .and(PipelineEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(PipelineEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(PipelineEntityKeys.accountId)
                                           .is(accountId),
        projectIdentifier, orgIdentifier, accountId, PipelineEntity.class);
  }
}
