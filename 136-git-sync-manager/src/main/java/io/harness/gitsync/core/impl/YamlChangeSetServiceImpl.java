package io.harness.gitsync.core.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.gitsync.common.beans.YamlChangeSet.MAX_RETRY_COUNT_EXCEEDED_CODE;
import static io.harness.gitsync.common.beans.YamlChangeSet.Status.QUEUED;
import static io.harness.gitsync.common.beans.YamlChangeSet.Status.RUNNING;
import static io.harness.gitsync.common.beans.YamlChangeSet.Status.SKIPPED;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.NoResultFoundException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetKeys;
import io.harness.gitsync.core.beans.GitSyncMetadata;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.core.service.YamlSuccessfulChangeService;
import io.harness.logging.ExceptionLogger;
import io.harness.repositories.yamlChangeSet.YamlChangeSetRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@ValidateOnExecution
@Slf4j
public class YamlChangeSetServiceImpl implements YamlChangeSetService {
  @Inject private YamlGitService yamlGitService;
  @Inject private YamlSuccessfulChangeService yamlSuccessfulChangeService;
  @Inject private YamlChangeSetRepository yamlChangeSetRepository;

  private static final Integer MAX_RETRY_COUNT = 3;

  @Override
  public YamlChangeSet save(YamlChangeSet yamlChangeSet) {
    populateGitSyncMetadata(yamlChangeSet);
    final YamlChangeSet savedYamlChangeSet = yamlChangeSetRepository.save(yamlChangeSet);
    onYamlChangeSetSave(savedYamlChangeSet);
    return savedYamlChangeSet;
  }

  private void onYamlChangeSetSave(YamlChangeSet savedYamlChangeSet) {
    try {
      if (isGitSyncConfiguredForChangeSet(savedYamlChangeSet)) {
        yamlSuccessfulChangeService.updateOnHarnessChangeSet(savedYamlChangeSet);
      }
    } catch (Exception e) {
      log.error("error while processing onYamlChangeSetSave event", e);
    }
  }

  @Override
  public void populateGitSyncMetadata(YamlChangeSet yamlChangeSet) {
    if (StringUtils.isBlank(yamlChangeSet.getQueueKey()) || yamlChangeSet.getGitSyncMetadata() == null) {
      try {
        final YamlGitConfigDTO yamlGitConfig = getYamlGitConfig(yamlChangeSet).get(0);

        yamlChangeSet.setGitSyncMetadata(buildGitSyncMetadata(yamlGitConfig));

        yamlChangeSet.setQueueKey(buildQueueKey(yamlGitConfig));
      } catch (Exception e) {
        log.warn("unable to populate git sync metadata. ignoring these fields", e);
      }
    }
  }

  private GitSyncMetadata buildGitSyncMetadata(YamlGitConfigDTO yamlGitConfig) {
    return GitSyncMetadata.builder()
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branchName(yamlGitConfig.getBranch())
        .repoName(yamlGitConfig.getRepo())
        .yamlGitConfigId(yamlGitConfig.getIdentifier())
        .build();
  }

  @NotNull
  private List<YamlGitConfigDTO> getYamlGitConfig(YamlChangeSet yamlChangeSet) {
    return yamlChangeSet.isGitToHarness() ? getYamlGitConfigForGitToHarness(yamlChangeSet)
                                          : Collections.singletonList(getYamlGitConfigForHarnessToGit(yamlChangeSet));
  }

  private String buildQueueKey(YamlGitConfigDTO yamlGitConfig) {
    return format(
        "%s:%s:%s", yamlGitConfig.getAccountId(), yamlGitConfig.getGitConnectorId(), yamlGitConfig.getBranch());
  }

  @NotNull
  private YamlGitConfigDTO getYamlGitConfigForHarnessToGit(YamlChangeSet yamlChangeSet) {
    // TODO(abhinav): expecting single file in git to harness
    final YamlGitConfigDTO yamlGitConfig = yamlGitService.getYamlGitConfigForHarnessToGitChangeSet(
        yamlChangeSet.getGitFileChanges().get(0), yamlChangeSet);
    if (yamlGitConfig == null) {
      throw NoResultFoundException.newBuilder()
          .message(format(
              "Unable to find yamlGitConfig for harness to git changeset for account =[%s], orgId=[%s]. projectId = [%s]Git Sync might not have been configured",
              yamlChangeSet.getAccountId(), yamlChangeSet.getOrganizationId(), yamlChangeSet.getProjectId()))
          .build();
    }
    return yamlGitConfig;
  }

  @NotNull
  private List<YamlGitConfigDTO> getYamlGitConfigForGitToHarness(YamlChangeSet yamlChangeSet) {
    final List<YamlGitConfigDTO> yamlGitConfigs =
        yamlGitService.getYamlGitConfigsForGitToHarnessChangeSet(yamlChangeSet);
    if (isEmpty(yamlGitConfigs)) {
      throw NoResultFoundException.newBuilder()
          .message(format(
              "unable to find yamlGitConfig for git to harness changeset for account =[%s], git connector id =[%s], branch=[%s]. Git Sync might not have been configured",
              yamlChangeSet.getAccountId(), yamlChangeSet.getGitWebhookRequestAttributes().getGitConnectorId(),
              yamlChangeSet.getGitWebhookRequestAttributes().getBranchName()))
          .build();
    }
    return yamlGitConfigs;
  }

  private boolean isGitSyncConfiguredForChangeSet(YamlChangeSet yamlChangeSet) {
    return yamlChangeSet.getGitSyncMetadata() != null;
  }

  @Override
  public Optional<YamlChangeSet> get(String accountId, String changeSetId) {
    return yamlChangeSetRepository.findById(changeSetId);
  }

  @Override
  public YamlChangeSet update(YamlChangeSet yamlChangeSet) {
    Objects.requireNonNull(yamlChangeSet.getUuid());
    return Optional.of(yamlChangeSetRepository.save(yamlChangeSet)).orElse(null);
  }

  @Override
  public synchronized YamlChangeSet getQueuedChangeSetForWaitingQueueKey(
      String accountId, String queueKey, int maxRunningChangesetsForAccount) {
    // TODO(abhinav): add persistent locker
    if (anyChangeSetRunningFoQueueKey(accountId, queueKey)) {
      log.info("Found running changeset for queuekey. Returning null");
      return null;
    }

    if (accountQuotaMaxedOut(accountId, maxRunningChangesetsForAccount)) {
      log.info("Account quota has been reached. Returning null");
      return null;
    }

    final YamlChangeSet selectedChangeSet = selectQueuedChangeSetWithPriority(accountId, queueKey);

    if (selectedChangeSet == null) {
      log.info("No change set found in queued state");
    }

    return selectedChangeSet;
  }

  private boolean accountQuotaMaxedOut(String accountId, int maxRunningChangesetsForAccount) {
    return yamlChangeSetRepository.countByAccountIdAndStatus(accountId, RUNNING) >= maxRunningChangesetsForAccount;
  }

  private YamlChangeSet selectQueuedChangeSetWithPriority(String accountId, String queueKey) {
    //      find the head of the queue
    YamlChangeSet selectedYamlChangeSet = null;

    final Optional<YamlChangeSet> headChangeSet = peekQueueHead(accountId, queueKey);
    if (headChangeSet.isPresent() && isFullSync(headChangeSet.get())) {
      selectedYamlChangeSet = headChangeSet.get();
    }

    if (selectedYamlChangeSet == null) {
      final Optional<YamlChangeSet> oldestGitToHarnessChangeSet = getOldestGitToHarnessChangeSet(accountId, queueKey);
      if (oldestGitToHarnessChangeSet.isPresent()) {
        selectedYamlChangeSet = oldestGitToHarnessChangeSet.get();
      }
    }

    if (selectedYamlChangeSet == null && headChangeSet.isPresent()) {
      selectedYamlChangeSet = headChangeSet.get();
    }

    if (selectedYamlChangeSet != null) {
      final boolean updateStatus = updateStatus(accountId, selectedYamlChangeSet.getUuid(), Status.RUNNING);
      if (updateStatus) {
        return get(accountId, selectedYamlChangeSet.getUuid()).orElse(null);
      } else {
        log.error("error while updating status of yaml change set Id = [{}]. Skipping selection",
            selectedYamlChangeSet.getUuid());
      }
    }
    return null;
  }

  private boolean isFullSync(YamlChangeSet yamlChangeSet) {
    return yamlChangeSet.isFullSync();
  }

  private Optional<YamlChangeSet> peekQueueHead(String accountId, String queueKey) {
    return yamlChangeSetRepository.findFirstByAccountIdAndQueueKeyAndStatusOrderByCreatedAt(
        accountId, queueKey, QUEUED);
  }

  private Optional<YamlChangeSet> getOldestGitToHarnessChangeSet(String accountId, String queueKey) {
    return yamlChangeSetRepository.findFirstAccountIdAndQueueKeyAndStatusAndGitToHarnessOrderByCreatedAt(
        accountId, queueKey, QUEUED, TRUE);
  }

  private boolean anyChangeSetRunningFoQueueKey(String accountId, String queueKey) {
    return yamlChangeSetRepository.countByAccountIdAndStatusAndQueueKey(accountId, RUNNING, queueKey) > 0;
  }
  @Override
  public boolean updateStatus(String accountId, String changeSetId, Status newStatus) {
    Optional<YamlChangeSet> yamlChangeSet = get(accountId, changeSetId);
    if (yamlChangeSet.isPresent()) {
      UpdateResult updateResult =
          yamlChangeSetRepository.updateYamlChangeSetStatus(newStatus, yamlChangeSet.get().getUuid());
      return updateResult.getModifiedCount() != 0;
    } else {
      log.warn("No YamlChangeSet found");
    }
    return false;
  }

  @Override
  public void markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(String accountId) {
    // TODO(abhinav): add persistent locker
    Update update = new Update()
                        .set(YamlChangeSetKeys.status, SKIPPED)
                        .set(YamlChangeSetKeys.messageCode, MAX_RETRY_COUNT_EXCEEDED_CODE);
    Query query = new Query().addCriteria(new Criteria()
                                              .and(YamlChangeSetKeys.accountId)
                                              .is(accountId)
                                              .and(YamlChangeSetKeys.status)
                                              .is(SKIPPED)
                                              .and(YamlChangeSetKeys.retryCount)
                                              .gt(MAX_RETRY_COUNT));
    UpdateResult status = yamlChangeSetRepository.update(query, update);
    log.info(
        "Updated the status of [{}] YamlChangeSets to Skipped. Max retry count exceeded", status.getModifiedCount());
  }

  @Override
  public boolean updateStatusAndIncrementRetryCountForYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatuses, List<String> yamlChangeSetIds) {
    // TODO(abhinav): add persistent locker
    if (isEmpty(yamlChangeSetIds)) {
      return true;
    }

    Update updateOps = new Update().set(YamlChangeSetKeys.status, newStatus);
    updateOps.inc(YamlChangeSetKeys.retryCount);

    Query query = new Query(new Criteria()
                                .and(YamlChangeSetKeys.accountId)
                                .is(accountId)
                                .and(YamlChangeSetKeys.status)
                                .in(currentStatuses)
                                .and(YamlChangeSetKeys.uuid)
                                .in(yamlChangeSetIds));

    return updateYamlChangeSets(accountId, query, updateOps);
  }

  @Override
  public boolean updateStatusForGivenYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatuses, List<String> yamlChangeSetIds) {
    // TODO(abhinav): add persistent locker

    if (isEmpty(yamlChangeSetIds)) {
      return true;
    }

    Query query = new Query().addCriteria(new Criteria()
                                              .and(YamlChangeSetKeys.accountId)
                                              .is(accountId)
                                              .and(YamlChangeSetKeys.status)
                                              .in(currentStatuses)
                                              .and(YamlChangeSetKeys.uuid)
                                              .in(yamlChangeSetIds));

    Update updateOps = new Update().set(YamlChangeSetKeys.status, newStatus);

    return updateYamlChangeSets(accountId, query, updateOps);
  }

  private boolean updateYamlChangeSets(String accountId, Query query, Update updateOperations) {
    try {
      UpdateResult status = yamlChangeSetRepository.update(query, updateOperations);
      return status.getModifiedCount() != 0;
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    } catch (Exception exception) {
      log.error("Error seen in fetching changeSet", exception);
    }

    return false;
  }

  @Override
  public List<YamlChangeSet> findByAccountIdsStatusLastUpdatedAtLessThan(
      List<String> runningAccountIdList, long timeout) {
    return yamlChangeSetRepository.findByAccountIdAndStatusAndLastUpdatedAtLessThan(
        runningAccountIdList, Status.RUNNING, System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(timeout));
  }

  @Override
  public List<String> findDistinctAccountIdsByStatus(Status status) {
    return yamlChangeSetRepository.findDistinctAccountIdByStatus(status);
  }

  @Override
  public UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
      Status oldStatus, Status newStatus, long timeout, String messageCode) {
    return yamlChangeSetRepository.updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
        oldStatus, newStatus, System.currentTimeMillis() - timeout, messageCode);
  }

  @Override
  public <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass) {
    return yamlChangeSetRepository.aggregate(aggregation, castClass);
  }
}
