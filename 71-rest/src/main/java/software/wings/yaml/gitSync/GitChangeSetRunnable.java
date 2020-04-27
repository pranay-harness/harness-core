package software.wings.yaml.gitSync;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFilename;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.service.impl.yaml.YamlProcessingLogContext.CHANGESET_ID;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.ProcessTimeLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author bsollish on 09/26/17
 */
@Slf4j
public class GitChangeSetRunnable implements Runnable {
  public static final List<Status> RUNNING_STATUS_LIST = singletonList(Status.RUNNING);
  private static AtomicLong lastTimestampForStuckJobCheck = new AtomicLong(0);

  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ConfigurationController configurationController;
  @Inject private GitChangeSetRunnableHelper gitChangeSetRunnableHelper;
  @Inject private PersistentLocker persistentLocker;

  @Override
  public void run() {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    logger.info(GIT_YAML_LOG_PREFIX + "Started job to pick changesets for processing");

    try {
      if (getMaintenanceFilename() || configurationController.isNotPrimary()) {
        logger.info("Not continuing with GitChangeSetRunnable job");
        return;
      }

      // TODO:: Use aggregate query for this group by??
      // Get ACCId list having yamlChangeSets in Queued state
      List<String> queuedAccountIdList = gitChangeSetRunnableHelper.getQueuedAccountIdList(wingsPersistence);

      // Get ACCId list having yamlChangeSets in Running state
      List<String> runningAccountIdList = gitChangeSetRunnableHelper.getRunningAccountIdList(wingsPersistence);

      // Check if any YamlChangeSet is stuck in Running state
      if (isNotEmpty(runningAccountIdList) && shouldPerformStuckJobCheck()) {
        lastTimestampForStuckJobCheck.set(System.currentTimeMillis());
        retryAnyStuckYamlChangeSet(runningAccountIdList);
      }

      List<String> waitingAccountIdList =
          queuedAccountIdList.stream().filter(accountId -> !runningAccountIdList.contains(accountId)).collect(toList());

      if (waitingAccountIdList.isEmpty()) {
        logger.info(GIT_YAML_LOG_PREFIX + "No waiting accounts found. Skip picking new changesets for processing");
        return;
      }

      logger.info(
          GIT_YAML_LOG_PREFIX + "queuedAccountIdList:[{}], runningAccountIdList:[{}], waitingAccountIdList:[{}]",
          queuedAccountIdList, runningAccountIdList, waitingAccountIdList);

      if (isNotEmpty(runningAccountIdList)) {
        logger.info(GIT_YAML_LOG_PREFIX
                + " Skipping processing of GitChangeSet for Accounts :[{}], as there is already running task for these accounts",
            runningAccountIdList);
      }

      waitingAccountIdList.forEach(accountId -> {
        YamlChangeSet queuedChangeSet = null;
        try (AccountLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
          queuedChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingAccount(accountId);
          if (queuedChangeSet != null) {
            logger.info(GIT_YAML_LOG_PREFIX + "Processing  " + CHANGESET_ID + ": [{}]", queuedChangeSet.getUuid());
            if (queuedChangeSet.isGitToHarness()) {
              yamlGitSyncService.handleGitChangeSet(queuedChangeSet, accountId);
            } else {
              yamlGitSyncService.handleHarnessChangeSet(queuedChangeSet, accountId);
            }
          } else {
            logger.info(GIT_YAML_LOG_PREFIX + "No change set queued to process for account");
          }
        } catch (Exception ex) {
          StringBuilder stringBuilder =
              new StringBuilder().append("Unexpected error while processing commit for accountId: ").append(accountId);
          if (queuedChangeSet != null) {
            yamlChangeSetService.updateStatusForYamlChangeSets(accountId, Status.FAILED, Status.RUNNING);
            stringBuilder.append(" and for " + CHANGESET_ID + ": ").append(queuedChangeSet.getUuid()).append("  ");
          }
          stringBuilder.append(" Reason: ").append(ExceptionUtils.getMessage(ex));
          logger.error(GIT_YAML_LOG_PREFIX + stringBuilder.toString(), ex);
        }
      });

      try (ProcessTimeLogContext ignore4 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        logger.info(GIT_YAML_LOG_PREFIX + "Successfully handled changsets for waiting accounts");
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error(GIT_YAML_LOG_PREFIX + "Unexpected error", exception);
    }
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
        gitChangeSetRunnableHelper.getStuckYamlChangeSets(wingsPersistence, runningAccountIdList);

    if (isNotEmpty(stuckChangeSets)) {
      // Map Acc vs such yamlChangeSets (with multigit support, there can be more than 1 for an account)
      Map<String, List<YamlChangeSet>> accountIdToStuckChangeSets =
          stuckChangeSets.stream().collect(Collectors.groupingBy(YamlChangeSet::getAccountId));

      // Mark these yamlChagneSets as Queued.
      accountIdToStuckChangeSets.forEach(this ::retryOrSkipStuckChangeSets);
    }
  }

  private void retryOrSkipStuckChangeSets(String accountId, List<YamlChangeSet> changeSets) {
    yamlChangeSetService.updateStatusAndIncrementRetryCountForYamlChangeSets(
        accountId, Status.QUEUED, RUNNING_STATUS_LIST, uuidsOfChangeSets(changeSets));

    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
  }

  @NotNull
  private List<String> uuidsOfChangeSets(List<YamlChangeSet> changeSets) {
    return changeSets.stream().map(YamlChangeSet::getUuid).collect(toList());
  }
}
