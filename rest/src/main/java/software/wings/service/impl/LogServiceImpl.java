package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.dl.HQuery.excludeAuthority;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.DataCleanUpJob;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class LogServiceImpl implements LogService {
  public static final int MAX_LOG_ROWS_PER_ACTIVITY = 1000;
  private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  public static final int NUM_OF_LOGS_TO_KEEP = 200;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ActivityService activityService;
  private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Log> list(PageRequest<Log> pageRequest) {
    return wingsPersistence.query(Log.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#save(software.wings.beans.Log)
   */
  @Override
  public void save(Log log) {
    batchedSave(singletonList(log));
  }

  @Override
  public CommandExecutionStatus getUnitExecutionResult(String appId, String activityId, String name) {
    Log log = wingsPersistence.createQuery(Log.class)
                  .filter("activityId", activityId)
                  .filter("appId", appId)
                  .filter("commandUnitName", name)
                  .field("commandExecutionStatus")
                  .exists()
                  .order("-lastUpdatedAt")
                  .get();
    return log != null && log.getCommandExecutionStatus() != null ? log.getCommandExecutionStatus() : RUNNING;
  }

  @Override
  public File exportLogs(String appId, String activityId) {
    File file = new File(
        Files.createTempDir(), format("ActivityLogs_%s.txt", dateFormatter.format(new Date(currentTimeMillis()))));
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(file), UTF_8)) {
      List<Log> logList =
          wingsPersistence.createQuery(Log.class).filter("appId", appId).filter("activityId", activityId).asList();
      for (Log log : logList) {
        fileWriter.write(format(
            "%s   %s   %s%n", log.getLogLevel(), dateFormatter.format(new Date(log.getCreatedAt())), log.getLogLine()));
      }
      return file;
    } catch (IOException ex) {
      throw new WingsException("Error in creating log file", ex);
    }
  }

  @Override
  public void pruneByActivity(String appId, String activityId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Log.class).filter("appId", appId).filter("activityId", activityId));
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public void batchedSave(List<Log> logs) {
    if (isNotEmpty(logs)) {
      logs = logs.stream().filter(Objects::nonNull).collect(toList());
      //    List<String> savedLogIds = wingsPersistence.save(logs);

      List<DBObject> dbObjects = new ArrayList<>(logs.size());
      for (Log log : logs) {
        try {
          DBObject dbObject = ((DatastoreImpl) wingsPersistence.getDatastore()).getMapper().toDBObject(log);
          dbObjects.add(dbObject);
        } catch (Exception e) {
          logger.error("Exception in saving log [{}]", log, e);
        }
      }
      WriteResult writeResult = wingsPersistence.getCollection("commandLogs").insert(dbObjects);

      // Map of [ActivityId -> [CommandUnitName -> LastLogLineStatus]]

      Map<String, Map<String, Log>> activityCommandUnitLastLogMap = logs.stream().collect(
          groupingBy(Log::getActivityId, toMap(Log::getCommandUnitName, Function.identity(), (l1, l2) -> {
            return l1.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)
                    || l1.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)
                ? l1
                : l2;
          })));

      //      Map<String, Map<String, Log>> activityCommandUnitLastLogMap =
      //          logs.stream().collect(groupingBy(Log::getActivityId, toMap(Log::getCommandUnitName,
      //          Function.identity(), (l1, l2) -> l2)));
      activityService.updateCommandUnitStatus(activityCommandUnitLastLogMap);
    }
  }

  @Override
  public void purgeActivityLogs() {
    long startTime = System.currentTimeMillis();
    logger.info("Purging activities Start time", startTime);
    final MorphiaIterator<Activity, Activity> nonPurgedActivityIterator =
        wingsPersistence.createQuery(Activity.class, excludeAuthority)
            .filter("logPurged", false)
            .field("createdAt")
            .greaterThan(System.currentTimeMillis() - DataCleanUpJob.LOGS_RETENTION_TIME)
            .fetchEmptyEntities();

    try (DBCursor ignored = nonPurgedActivityIterator.getCursor()) {
      while (nonPurgedActivityIterator.hasNext()) {
        String activityId = nonPurgedActivityIterator.next().getUuid();
        List<String> idsToKeep = new ArrayList<>();
        final MorphiaIterator<Log, Log> logIterator = wingsPersistence.createQuery(Log.class, excludeAuthority)
                                                          .filter("activityId", activityId)
                                                          .order("-createdAt")
                                                          .fetchEmptyEntities();
        try (DBCursor cursor = logIterator.getCursor()) {
          while (logIterator.hasNext()) {
            idsToKeep.add(logIterator.next().getUuid());
            if (idsToKeep.size() >= NUM_OF_LOGS_TO_KEEP) {
              break;
            }
          }
        }
        if (idsToKeep.size() != 0) {
          logger.info("Deleting logs of size {} for activityId {}", idsToKeep.size(), activityId);
          wingsPersistence.delete(wingsPersistence.createQuery(Log.class, excludeAuthority)
                                      .filter("activityId", activityId)
                                      .field("_id")
                                      .hasNoneOf(idsToKeep));
        }
        // TODO: We should enable this and run it once. Also, We have not purged the command logs for a long time
        /* logger.info("Updating activityId {} logPurged status to true", activityId);
         Query<Activity> query =
             wingsPersistence.createQuery(Activity.class).filter(Mapper.ID_KEY, activityId).disableValidation();
         UpdateOperations<Activity> updateOperations =
             wingsPersistence.createUpdateOperations(Activity.class).disableValidation().set("logPurged", true);
         wingsPersistence.update(query, updateOperations);
         logger.info("Updating activityId {} logPurged status to true success", activityId);*/
      }
    }
    logger.info("Purging activities end time", System.currentTimeMillis() - startTime);
  }

  @Override
  public String batchedSaveCommandUnitLogs(String activityId, String unitName, Log log) {
    if (log.getCommandExecutionStatus().equals(RUNNING)) {
      // only RUNNING status will be counted for  MAX_LOG_ROWS_PER_ACTIVITY threshold
      long count = wingsPersistence.createQuery(Log.class)
                       .filter("appId", log.getAppId())
                       .filter("activityId", activityId)
                       .count();
      if (count > MAX_LOG_ROWS_PER_ACTIVITY) {
        logger.warn(
            "Number of log rows per activity threshold [{}] crossed. [{}] log lines truncated for activityId: [{}], commandUnitName: [{}]",
            MAX_LOG_ROWS_PER_ACTIVITY, log.getLinesCount(), log.getActivityId(), log.getCommandUnitName());
        activityService.updateCommandUnitStatus(log.getAppId(), activityId, unitName, log.getCommandExecutionStatus());
        return null;
      }
    }

    String logId = wingsPersistence.save(log);
    activityService.updateCommandUnitStatus(log.getAppId(), activityId, unitName, log.getCommandExecutionStatus());
    return logId;
  }
}
