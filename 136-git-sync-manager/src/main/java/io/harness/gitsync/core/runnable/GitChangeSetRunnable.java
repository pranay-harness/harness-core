package io.harness.gitsync.core.runnable;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import io.harness.exception.WingsException;
import io.harness.gitsync.common.YamlProcessingLogContext;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetKeys;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.ProcessTimeLogContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitChangeSetRunnable implements Runnable {
  public static final List<Status> RUNNING_STATUS_LIST = singletonList(Status.RUNNING);
  public static final int MAX_RUNNING_CHANGESETS_FOR_ACCOUNT = 5;
  private static final AtomicLong lastTimestampForStuckJobCheck = new AtomicLong(0);
  private YamlGitService yamlGitSyncService;
  private YamlChangeSetService yamlChangeSetService;
  private GitChangeSetRunnableHelper gitChangeSetRunnableHelper;

  @Override
  public void run() {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    log.info(GIT_YAML_LOG_PREFIX + "Started job to pick changesets for processing");

    try {
      if (!shouldRun()) {
        log.info("Not continuing with GitChangeSetRunnable job");
        return;
      }

      handleStuckChangeSets();

      final List<YamlChangeSet> yamlChangeSets = getYamlChangeSetsToProcess();

      if (yamlChangeSets.isEmpty()) {
        log.info("No changesets found for processing in this run");
      } else {
        log.info("changesets to process =[{}]", yamlChangeSets.stream().map(YamlChangeSet::getUuid).collect(toList()));

        yamlChangeSets.forEach(this::processChangeSet);
      }

      try (ProcessTimeLogContext ignore4 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        log.info(GIT_YAML_LOG_PREFIX + "Successfully handled changesets for waiting accounts");
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    } catch (Exception exception) {
      log.error(GIT_YAML_LOG_PREFIX + "Unexpected error", exception);
    }
  }

  private List<YamlChangeSet> getYamlChangeSetsToProcess() {
    return getYamlChangeSetsPerQueueKey();
  }

  private AutoLogContext createLogContextForChangeSet(YamlChangeSet yamlChangeSet) {
    return YamlProcessingLogContext.builder()
        .changeSetQueueKey(yamlChangeSet.getQueueKey())
        .changeSetId(yamlChangeSet.getUuid())
        .build(OVERRIDE_ERROR);
  }

  private void processChangeSet(YamlChangeSet yamlChangeSet) {
    final String accountId = yamlChangeSet.getAccountId();
    try (AccountLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = createLogContextForChangeSet(yamlChangeSet)) {
      log.info("GIT_YAML_LOG_ENTRY: Processing  changeSetId: [{}]", yamlChangeSet.getUuid());

      if (yamlChangeSet.isGitToHarness()) {
        yamlGitSyncService.handleGitChangeSet(yamlChangeSet, accountId);
      } else {
        yamlGitSyncService.handleHarnessChangeSet(yamlChangeSet, accountId);
      }
    } catch (Exception ex) {
      log.error(format("Unexpected error while processing commit for accountId: [%s], changeSetId =[%s] ", accountId,
                    yamlChangeSet.getUuid()),
          ex);
      yamlChangeSetService.updateStatusForGivenYamlChangeSets(
          accountId, Status.FAILED, RUNNING_STATUS_LIST, singletonList(yamlChangeSet.getUuid()));
    }
  }

  @NotNull
  private List<YamlChangeSet> getYamlChangeSetsPerQueueKey() {
    final Set<ChangeSetGroupingKey> queuedChangeSetKeys = getQueuedChangesetKeys();
    final Set<ChangeSetGroupingKey> runningChangeSetKeys = getRunningChangesetKeys();
    final Set<String> maxedOutAccountIds = getMaxedOutAccountIds(runningChangeSetKeys);
    final Set<ChangeSetGroupingKey> eligibleChangeSetKeysForPicking =
        getEligibleQueueKeysForPicking(queuedChangeSetKeys, runningChangeSetKeys, maxedOutAccountIds);

    log.info(GIT_YAML_LOG_PREFIX
            + "queuedChangeSetKeys:{}, runningChangeSetKeys:{}, maxedOutAccountIds: {} ,eligibleChangeSetKeysForPicking:{}",
        queuedChangeSetKeys, runningChangeSetKeys, maxedOutAccountIds, eligibleChangeSetKeysForPicking);

    if (isNotEmpty(maxedOutAccountIds)) {
      log.info(GIT_YAML_LOG_PREFIX
              + " Skipping processing of GitChangeSet for Accounts :[{}], as concurrently running tasks have maxed out",
          maxedOutAccountIds);
    }

    return eligibleChangeSetKeysForPicking.stream()
        .map(changeSetGroupingKey
            -> getQueuedChangeSetForWaitingQueueKey(
                changeSetGroupingKey.getAccountId(), changeSetGroupingKey.getQueueKey()))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private YamlChangeSet getQueuedChangeSetForWaitingQueueKey(String accountId, String queueKey) {
    try (
        AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
        AutoLogContext ignore2 = YamlProcessingLogContext.builder().changeSetQueueKey(queueKey).build(OVERRIDE_ERROR)) {
      final YamlChangeSet yamlChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(
          accountId, queueKey, getMaxRunningChangesetsForAccount());
      if (yamlChangeSet == null) {
        log.info("no changeset found to process");
      }
      return yamlChangeSet;
    } catch (Exception ex) {
      log.error(
          format("error while finding changeset to process for accountId=[%s], queueKey=[%s]", accountId, queueKey),
          ex);
    }
    return null;
  }

  private boolean shouldRun() {
    // TODO(abhinav): add maintainance logic
    //    return !getMaintenanceFilename() && configurationController.isPrimary();
    return true;
  }

  private void handleStuckChangeSets() {
    if (shouldPerformStuckJobCheck()) {
      log.info("handling stuck change sets");
      lastTimestampForStuckJobCheck.set(System.currentTimeMillis());
      gitChangeSetRunnableHelper.handleOldQueuedChangeSets(yamlChangeSetService);
      handleStuckRunningChangesets();
      log.info("Successfully handled stuck change sets");
    }
  }

  private void handleStuckRunningChangesets() {
    List<String> runningAccountIdList = gitChangeSetRunnableHelper.getRunningAccountIdList(yamlChangeSetService);
    retryAnyStuckYamlChangeSet(runningAccountIdList);
  }

  /**
   * This job runs every few seconds. We dont need to check for stuck job every time.
   * We will check it every 30 mins.
   */
  boolean shouldPerformStuckJobCheck() {
    return lastTimestampForStuckJobCheck.get() == 0
        || (System.currentTimeMillis() - lastTimestampForStuckJobCheck.get() > TimeUnit.MINUTES.toMillis(30));
  }

  /**
   * If any YamlChangeSet is stuck in Running mode for more than 90 minutes
   * (somehow delegate response was lost or something, mark that changeset as Queued again)
   * So let it be processed again as we don't know if that was applied.
   * If it was already applied, delegate won't do anything.
   */
  void retryAnyStuckYamlChangeSet(List<String> runningAccountIdList) {
    if (isEmpty(runningAccountIdList)) {
      return;
    }

    // Get yamlChangeSet that is in running mode for more than 90 mins.
    List<YamlChangeSet> stuckChangeSets =
        gitChangeSetRunnableHelper.getStuckYamlChangeSets(yamlChangeSetService, runningAccountIdList);

    if (isNotEmpty(stuckChangeSets)) {
      // Map Acc vs such yamlChangeSets (with multigit support, there can be more than 1 for an account)
      Map<String, List<YamlChangeSet>> accountIdToStuckChangeSets =
          stuckChangeSets.stream().collect(Collectors.groupingBy(YamlChangeSet::getAccountId));

      // Mark these yamlChagneSets as Queued.
      accountIdToStuckChangeSets.forEach(this::retryOrSkipStuckChangeSets);
    }
  }

  private void retryOrSkipStuckChangeSets(String accountId, List<YamlChangeSet> changeSets) {
    final List<String> yamlChangeSetIds = uuidsOfChangeSets(changeSets);
    yamlChangeSetService.updateStatusAndIncrementRetryCountForYamlChangeSets(
        accountId, Status.QUEUED, RUNNING_STATUS_LIST, yamlChangeSetIds);
    log.info("Retrying stuck changesets: [{}]", yamlChangeSetIds);

    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
  }

  @NotNull
  private List<String> uuidsOfChangeSets(List<YamlChangeSet> changeSets) {
    return changeSets.stream().map(YamlChangeSet::getUuid).collect(toList());
  }

  private Set<String> getMaxedOutAccountIds(Set<ChangeSetGroupingKey> runningQueueKeys) {
    return runningQueueKeys.stream()
        .collect(Collectors.groupingBy(
            ChangeSetGroupingKey::getAccountId, Collectors.summingInt(ChangeSetGroupingKey::getCount)))
        .entrySet()
        .stream()
        .filter(accountIdTotalCountEntry -> accountIdTotalCountEntry.getValue() >= getMaxRunningChangesetsForAccount())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private Set<ChangeSetGroupingKey> getEligibleQueueKeysForPicking(Set<ChangeSetGroupingKey> queuedChangesetKeys,
      Set<ChangeSetGroupingKey> runningQueueKeys, Set<String> maxedOutAccountIds) {
    return Sets.difference(queuedChangesetKeys, runningQueueKeys)
        .stream()
        .filter(changeSetGroupingKey -> !maxedOutAccountIds.contains(changeSetGroupingKey.getAccountId()))
        .collect(Collectors.toSet());
  }

  private Set<ChangeSetGroupingKey> getQueuedChangesetKeys() {
    Criteria criteria = Criteria.where(YamlChangeSetKeys.status).is(Status.QUEUED);
    return getChangesetGroupingKeys(criteria);
  }

  private Set<ChangeSetGroupingKey> getRunningChangesetKeys() {
    Criteria criteria = Criteria.where(YamlChangeSetKeys.status).is(Status.RUNNING);
    return getChangesetGroupingKeys(criteria);
  }

  @NotNull
  private Set<ChangeSetGroupingKey> getChangesetGroupingKeys(Criteria criteria) {
    Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
        Aggregation.group(YamlChangeSetKeys.accountId, YamlChangeSetKeys.queueKey)
            .first(YamlChangeSetKeys.accountId)
            .as(YamlChangeSetKeys.accountId)
            .first(YamlChangeSetKeys.queueKey)
            .as(YamlChangeSetKeys.queueKey)
            .count()
            .as("count"));
    AggregationResults<ChangeSetGroupingKey> aggregationResults =
        yamlChangeSetService.aggregate(aggregation, ChangeSetGroupingKey.class);

    final Set<ChangeSetGroupingKey> keys = new HashSet<>();
    aggregationResults.iterator().forEachRemaining(keys::add);
    return keys;
  }

  @VisibleForTesting
  int getMaxRunningChangesetsForAccount() {
    return MAX_RUNNING_CHANGESETS_FOR_ACCOUNT;
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  public static class ChangeSetGroupingKey {
    @Include String accountId;
    @Include String queueKey;
    int count;

    @Override
    public String toString() {
      return "{"
          + "accountId='" + accountId + '\'' + ", queueKey='" + queueKey + '\'' + ", count=" + count + '}';
    }
  }
}
