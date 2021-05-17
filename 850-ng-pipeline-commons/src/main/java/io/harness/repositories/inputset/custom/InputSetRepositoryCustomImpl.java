package io.harness.repositories.inputset.custom;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.inputset.mappers.InputSetFilterHelper;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;

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
@ToBeDeleted
@Deprecated
public class InputSetRepositoryCustomImpl implements InputSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public Page<BaseInputSetEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<BaseInputSetEntity> projects = mongoTemplate.find(query, BaseInputSetEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), BaseInputSetEntity.class));
  }

  @Override
  public BaseInputSetEntity update(Criteria criteria, BaseInputSetEntity baseInputSetEntity) {
    Query query = new Query(criteria);
    Update update = InputSetFilterHelper.getUpdateOperations(baseInputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Service; attempt: {}", "[Failed]: Failed updating Service; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), BaseInputSetEntity.class));
  }

  @Override
  public UpdateResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    Update updateOperationsForDelete = InputSetFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Service; attempt: {}", "[Failed]: Failed deleting Service; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(() -> mongoTemplate.updateFirst(query, updateOperationsForDelete, BaseInputSetEntity.class));
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
