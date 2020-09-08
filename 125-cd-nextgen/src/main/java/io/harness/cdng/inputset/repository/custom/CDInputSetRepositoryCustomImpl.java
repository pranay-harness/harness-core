package io.harness.cdng.inputset.repository.custom;

import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.mappers.CDInputSetFilterHelper;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.time.Duration;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class CDInputSetRepositoryCustomImpl implements CDInputSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public Page<CDInputSetEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<CDInputSetEntity> projects = mongoTemplate.find(query, CDInputSetEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CDInputSetEntity.class));
  }

  @Override
  public UpdateResult upsert(Criteria criteria, CDInputSetEntity cdInputSetEntity) {
    Query query = new Query(criteria);
    Update update = CDInputSetFilterHelper.getUpdateOperations(cdInputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed upserting Service; attempt: {}", "[Failed]: Failed upserting Service; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.upsert(query, update, CDInputSetEntity.class));
  }

  @Override
  public UpdateResult update(Criteria criteria, CDInputSetEntity cdInputSetEntity) {
    Query query = new Query(criteria);
    Update update = CDInputSetFilterHelper.getUpdateOperations(cdInputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Service; attempt: {}", "[Failed]: Failed updating Service; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.updateFirst(query, update, CDInputSetEntity.class));
  }

  @Override
  public UpdateResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    Update updateOperationsForDelete = ServiceFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Service; attempt: {}", "[Failed]: Failed deleting Service; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(() -> mongoTemplate.updateFirst(query, updateOperationsForDelete, CDInputSetEntity.class));
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> logger.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> logger.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
