package software.wings.yaml.gitSync;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.utils.Misc;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bsollish on 09/26/17
 */
public class GitChangeSetRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GitChangeSetRunnable.class);

  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ConfigurationController configurationController;

  @Override
  public void run() {
    try {
      if (isMaintenance() || configurationController.isNotPrimary()) {
        return;
      }

      // TODO:: Use aggregate query for this group by??
      List<String> queuedAccountIdList =
          wingsPersistence.getDatastore()
              .getCollection(YamlChangeSet.class)
              .distinct("accountId",
                  new BasicDBObject(
                      "status", new BasicDBObject("$in", new String[] {Status.FAILED.name(), Status.QUEUED.name()})));
      List<String> runningAccountIdList =
          wingsPersistence.getDatastore()
              .getCollection(YamlChangeSet.class)
              .distinct("accountId", new BasicDBObject("status", Status.RUNNING.name()));
      List<String> waitingAccountIdList =
          queuedAccountIdList.stream().filter(accountId -> !runningAccountIdList.contains(accountId)).collect(toList());

      if (waitingAccountIdList.isEmpty()) {
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
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error(GIT_YAML_LOG_PREFIX + "Unexpected error", exception);
    }
  }
}
