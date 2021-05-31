package io.harness.repositories.sdk;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstance.PmsSdkInstanceKeys;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class PmsSdkInstanceRepositoryCustomImpl implements PmsSdkInstanceRepositoryCustom {
  private static final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(5);
  private static final int MAX_ATTEMPTS = 3;

  private final MongoTemplate mongoTemplate;

  @Override
  public void updatePmsSdkInstance(InitializeSdkRequest request, Map<String, Set<String>> supportedTypes) {
    String name = request.getName();
    List<StepInfo> supportedSteps = request.getSupportedStepsList();
    List<StepType> supportedStepTypes = request.getSupportedStepTypesList();
    Query query = query(Criteria.where(PmsSdkInstanceKeys.name).is(name));
    Update update =
        update(PmsSdkInstanceKeys.supportedTypes, supportedTypes)
            .set(PmsSdkInstanceKeys.supportedSteps, supportedSteps)
            .set(PmsSdkInstanceKeys.supportedStepTypes, supportedStepTypes)
            .set(PmsSdkInstanceKeys.lastUpdatedAt, System.currentTimeMillis())
            .set(PmsSdkInstanceKeys.interruptConsumerConfig, request.getInterruptConsumerConfig())
            .set(PmsSdkInstanceKeys.orchestrationEventConsumerConfig, request.getOrchestrationEventConsumerConfig())
            .set(PmsSdkInstanceKeys.sdkModuleInfo, request.getSdkModuleInfo());
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed updating PMS SDK instance; attempt: {}",
        "[Failed]: Failed updating PMS SDK instance; attempt: {}");
    Failsafe.with(retryPolicy).get(() -> mongoTemplate.findAndModify(query, update, PmsSdkInstance.class));
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
