package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
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

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSInputSetRepositoryCustomImpl implements PMSInputSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public InputSetEntity update(Criteria criteria, InputSetEntity inputSetEntity) {
    Query query = new Query(criteria);
    Update update = PMSInputSetFilterHelper.getUpdateOperations(inputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Input Set; attempt: {}", "[Failed]: Failed updating Input Set; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), InputSetEntity.class));
  }

  @Override
  public UpdateResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    Update update = PMSInputSetFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Input Set; attempt: {}", "[Failed]: Failed deleting Input Set; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.updateFirst(query, update, InputSetEntity.class));
  }

  @Override
  public UpdateResult deleteAllInputSetsWhenPipelineDeleted(Query query, Update update) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Input Set; attempt: {}", "[Failed]: Failed deleting Input Set; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.updateMulti(query, update, InputSetEntity.class));
  }

  @Override
  public Page<InputSetEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<InputSetEntity> inputSetEntities = mongoTemplate.find(query, InputSetEntity.class);
    return PageableExecutionUtils.getPage(inputSetEntities, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), InputSetEntity.class));
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
