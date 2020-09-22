package io.harness.ngpipeline.overlayinputset.repository.custom;

import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetFilterHelper;
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
public class OverlayInputSetRepositoryCustomImpl implements OverlayInputSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public Page<OverlayInputSetEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<OverlayInputSetEntity> projects = mongoTemplate.find(query, OverlayInputSetEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), OverlayInputSetEntity.class));
  }

  @Override
  public UpdateResult upsert(Criteria criteria, OverlayInputSetEntity overlayInputSetEntity) {
    Query query = new Query(criteria);
    Update update = OverlayInputSetFilterHelper.getUpdateOperations(overlayInputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed upserting OverlayInputSet; attempt: {}",
        "[Failed]: Failed upserting OverlayInputSet; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.upsert(query, update, OverlayInputSetEntity.class));
  }

  @Override
  public UpdateResult update(Criteria criteria, OverlayInputSetEntity overlayInputSetEntity) {
    Query query = new Query(criteria);
    Update update = OverlayInputSetFilterHelper.getUpdateOperations(overlayInputSetEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed updating OverlayInputSet; attempt: {}",
        "[Failed]: Failed updating OverlayInputSet; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.updateFirst(query, update, OverlayInputSetEntity.class));
  }

  @Override
  public UpdateResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    Update updateOperationsForDelete = OverlayInputSetFilterHelper.getUpdateOperationsForDelete();
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed deleting OverlayInputSet; attempt: {}",
        "[Failed]: Failed deleting OverlayInputSet; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(() -> mongoTemplate.updateFirst(query, updateOperationsForDelete, OverlayInputSetEntity.class));
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
