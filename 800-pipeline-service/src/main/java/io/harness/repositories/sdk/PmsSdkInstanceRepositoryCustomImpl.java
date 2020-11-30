package io.harness.repositories.sdk;

import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstance.PmsSdkInstanceKeys;
import io.harness.pms.steps.StepInfo;

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

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class PmsSdkInstanceRepositoryCustomImpl implements PmsSdkInstanceRepositoryCustom {
  private static final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(5);
  private static final int MAX_ATTEMPTS = 3;

  private final MongoTemplate mongoTemplate;

  @Override
  public void updatePmsSdkInstance(
      String name, Map<String, Set<String>> supportedTypes, List<StepInfo> supportedSteps) {
    Query query = Query.query(Criteria.where(PmsSdkInstanceKeys.name).is(name));
    Update update = Update.update(PmsSdkInstanceKeys.supportedTypes, supportedTypes)
                        .set(PmsSdkInstanceKeys.supportedSteps, supportedSteps);
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
