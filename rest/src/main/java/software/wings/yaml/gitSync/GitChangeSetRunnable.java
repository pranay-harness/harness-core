package software.wings.yaml.gitSync;

import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bsollish on 09/26/17
 */
public class GitChangeSetRunnable implements Runnable {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void run() {
    try {
      if (isMaintenance()) {
        return;
      }

      // TODO:: Use aggregate query for this group by??
      List<String> queuedAccountIdList = wingsPersistence.getDatastore()
                                             .getCollection(YamlChangeSet.class)
                                             .distinct("accountId", new BasicDBObject("status", Status.QUEUED.name()));
      List<String> runningAccountIdList =
          wingsPersistence.getDatastore()
              .getCollection(YamlChangeSet.class)
              .distinct("accountId", new BasicDBObject("status", Status.RUNNING.name()));
      List<String> waitingAccountIdList = queuedAccountIdList.stream()
                                              .filter(accountId -> !runningAccountIdList.contains(accountId))
                                              .collect(Collectors.toList());

      logger.info("queuedAccountIdList:[{}], runningAccountIdList:[{}], waitingAccountIdList:[{}]", queuedAccountIdList,
          runningAccountIdList, waitingAccountIdList);

      if (waitingAccountIdList.size() == 0) {
        logger.info("No changeSet can be processed");
        // check and reset/expire if needed
      } else {
        // nothing already in execution and lock acquired
        waitingAccountIdList.forEach(accountId -> {
          YamlChangeSet queuedChangeSet = yamlChangeSetService.getQueuedChangeSet(accountId);
          if (queuedChangeSet != null) {
            logger.info("Processing ChangeSet {}", queuedChangeSet);
            yamlGitSyncService.handleChangeSet(queuedChangeSet);
          } else {
            logger.info("No change set queued to process for accountId [{}]", accountId);
          }
        });
      }
    } catch (Exception ex) {
      logger.error("Unexpected error ", ex);
    }
  }
}
