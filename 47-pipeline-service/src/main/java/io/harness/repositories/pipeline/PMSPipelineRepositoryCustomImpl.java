package io.harness.repositories.pipeline;

import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;

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
public class PMSPipelineRepositoryCustomImpl implements PMSPipelineRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public PipelineEntity update(Criteria criteria, PipelineEntity pipelineEntity) {
    Query query = new Query(criteria);
    Update update = PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Pipeline; attempt: {}", "[Failed]: Failed updating Pipeline; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), PipelineEntity.class));
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
  public Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<PipelineEntity> projects = mongoTemplate.find(query, PipelineEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), PipelineEntity.class));
  }
}
