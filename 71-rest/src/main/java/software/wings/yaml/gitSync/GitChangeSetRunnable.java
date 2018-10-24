package software.wings.yaml.gitSync;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.utils.Misc;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author bsollish on 09/26/17
 */
public class GitChangeSetRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GitChangeSetRunnable.class);
  private static AtomicLong lastTimestampForStuckJobCheck = new AtomicLong(0);

  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ConfigurationController configurationController;
  @Inject private GitChangeSetRunnableHelper gitChangeSetRunnableHelper;

  @Override
  public void run() {
    try {
      if (isMaintenance() || configurationController.isNotPrimary()) {
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
        logger.info("No Accounts have YamlChangeSets that need processing");
        return;
      }

      logger.info(
          GIT_YAML_LOG_PREFIX + "queuedAccountIdList:[{}], runningAccountIdList:[{}], waitingAccountIdList:[{}]",
          queuedAccountIdList, runningAccountIdList, waitingAccountIdList);

      // @TODO
      // We should check here or create a cron job, that will check periodically if there is any task running for more
      // than 10 mins (same as timeout duration for GitDelegateTask), just mark it as failed, so other YamlChangeSets
      // for that account get unblocked
      if (isNotEmpty(runningAccountIdList)) {
        logger.info(GIT_YAML_LOG_PREFIX
                + " Skipping processing of GitChangeSet for Accounts :[{}], as there is already running task for these accounts",
            runningAccountIdList);
      }

      // nothing already in execution and lock acquired
      waitingAccountIdList.forEach(accountId -> {
        List<YamlChangeSet> queuedChangeSets = new ArrayList<>();
        try {
          if (featureFlagService.isEnabled(FeatureName.GIT_BATCH_SYNC, accountId)) {
            queuedChangeSets = yamlChangeSetService.getChangeSetsToSync(accountId);
          } else {
            queuedChangeSets = yamlChangeSetService.getQueuedChangeSet(accountId);
          }
          if (isNotEmpty(queuedChangeSets)) {
            StringBuilder builder =
                new StringBuilder("Processing ChangeSets: for Account: ").append(accountId).append("\n ");
            queuedChangeSets.forEach(yamlChangeSet -> builder.append(yamlChangeSet).append("\n"));
            logger.info(GIT_YAML_LOG_PREFIX + builder.toString());
            yamlGitSyncService.handleChangeSet(queuedChangeSets, accountId);
          } else {
            logger.info(GIT_YAML_LOG_PREFIX + "No change set queued to process for accountId [{}]", accountId);
          }
        } catch (Exception ex) {
          StringBuilder stringBuilder =
              new StringBuilder().append("Unexpected error while processing commit for accountId: ").append(accountId);
          if (queuedChangeSets != null) {
            yamlChangeSetService.updateStatusForYamlChangeSets(accountId, Status.FAILED, Status.RUNNING);
            StringBuilder builder = new StringBuilder();
            queuedChangeSets.stream().map(yamlChangeSet -> builder.append(yamlChangeSet.getUuid()).append("  "));
            stringBuilder.append(" and for changeSet: ").append(builder.toString());
          }
          stringBuilder.append(" Reason: ").append(Misc.getMessage(ex));
          logger.error(GIT_YAML_LOG_PREFIX + stringBuilder.toString(), ex);
        }
      });
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error(GIT_YAML_LOG_PREFIX + "Unexpected error", exception);
    }
  }

  /**
   * This job runs every few seconds. We dont need to check for stuck job every time.
   * We will check it every 30 mins.
   * @return
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
   * @param runningAccountIdList
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
          stuckChangeSets.stream().collect(Collectors.groupingBy(yamlChangeSet -> yamlChangeSet.getAccountId()));

      // Mark these yamlChagneSets as Queued.
      accountIdToStuckChangeSets.forEach(
          (k, v)
              -> yamlChangeSetService.updateStatusForGivenYamlChangeSets(k, Status.QUEUED,
                  Arrays.asList(Status.RUNNING),
                  v.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(toList())));
    }
  }
}
