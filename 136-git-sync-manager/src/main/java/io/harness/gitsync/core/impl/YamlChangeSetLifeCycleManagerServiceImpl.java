package io.harness.gitsync.core.impl;

import static io.harness.gitsync.common.beans.YamlChangeSetStatus.getTerminalStatusList;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.gitsync.common.YamlProcessingLogContext;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetHandler;
import io.harness.gitsync.core.service.YamlChangeSetLifeCycleManagerService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class YamlChangeSetLifeCycleManagerServiceImpl implements YamlChangeSetLifeCycleManagerService {
  private static final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(1);
  private static final int MAX_ATTEMPTS = 3;

  private final ExecutorService executorService;
  private final YamlChangeSetEventHandlerFactory yamlChangeSetHandlerFactory;
  private final YamlChangeSetService yamlChangeSetService;

  @Override
  public void handleChangeSet(YamlChangeSetDTO yamlChangeSet) {
    final YamlChangeSetHandler changeSetHandler = yamlChangeSetHandlerFactory.getChangeSetHandler(yamlChangeSet);
    executorService.submit(() -> {
      try (AccountLogContext ignore1 = new AccountLogContext(yamlChangeSet.getAccountId(), OVERRIDE_ERROR);
           AutoLogContext ignore2 = createLogContextForChangeSet(yamlChangeSet)) {
        final YamlChangeSetStatus status = changeSetHandler.process(yamlChangeSet);
        handleChangeSetStatus(yamlChangeSet, status);
      } catch (Exception e) {
        log.error("Exception occurred while handling changeset: [{}]", yamlChangeSet.getChangesetId(), e);
        handleFailure(yamlChangeSet);
      }
    });
  }

  private void handleFailure(YamlChangeSetDTO yamlChangeSet) {
    final RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying] attempt: {} for failure case of changeset update.",
            "[Failed] attempt: {} for failure case of changeset update.");
    final boolean success =
        Failsafe.with(retryPolicy)
            .get(()
                     -> yamlChangeSetService.updateStatusWithRetryCountIncrement(
                         yamlChangeSet.getAccountId(), YamlChangeSetStatus.QUEUED, yamlChangeSet.getChangesetId()));
    log.info("Update status result: [{}] while updating status to QUEUED for changesetId [{}]",
        success ? "success" : "failure", yamlChangeSet.getChangesetId());
  }

  private void handleChangeSetStatus(YamlChangeSetDTO yamlChangeSet, YamlChangeSetStatus status) {
    final List<YamlChangeSetStatus> completedStatusList = getTerminalStatusList();
    if (!completedStatusList.contains(status)) {
      log.error("Encountered incorrect status: [{}]", status);
      // In case of status not in completed status marking changeset as failed.
      handleFailure(yamlChangeSet);
    }
    final RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying] attempt: {} for failure case of changeset update.",
            "[Failed] attempt: {} for failure case of changeset update.");
    final boolean success = Failsafe.with(retryPolicy)
                                .get(()
                                         -> yamlChangeSetService.updateStatus(
                                             yamlChangeSet.getAccountId(), yamlChangeSet.getChangesetId(), status));
    log.info("Update status result: [{}] attempting status update to [{}] for changesetId [{}]",
        success ? "success" : "failure", status, yamlChangeSet.getChangesetId());
  }

  private AutoLogContext createLogContextForChangeSet(YamlChangeSetDTO yamlChangeSet) {
    return YamlProcessingLogContext.builder().changeSetId(yamlChangeSet.getChangesetId()).build(OVERRIDE_ERROR);
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
