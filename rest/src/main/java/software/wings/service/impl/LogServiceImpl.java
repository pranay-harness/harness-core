package software.wings.service.impl;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;

import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.SortOrder.OrderType;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class LogServiceImpl implements LogService {
  private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  public static final int NUM_OF_LOGS_TO_KEEP = 200;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ActivityService activityService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Log> list(String appId, String activityId, String unitName, PageRequest<Log> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("activityId", activityId, EQ);
    pageRequest.addFilter("commandUnitName", unitName, EQ);
    pageRequest.addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build());
    pageRequest.setLimit(String.valueOf(NUM_OF_LOGS_TO_KEEP));
    PageResponse<Log> response = wingsPersistence.query(Log.class, pageRequest);

    if (response != null) {
      Collections.reverse(response.getResponse());
    }
    return response;
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
                  .field("activityId")
                  .equal(activityId)
                  .field("appId")
                  .equal(appId)
                  .field("commandUnitName")
                  .equal(name)
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
    try {
      OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
      List<Log> logList = wingsPersistence.createQuery(Log.class)
                              .field("appId")
                              .equal(appId)
                              .field("activityId")
                              .equal(activityId)
                              .asList();
      for (Log log : logList) {
        fileWriter.write(format(
            "%s   %s   %s\n", log.getLogLevel(), dateFormatter.format(new Date(log.getCreatedAt())), log.getLogLine()));
      }
      fileWriter.close();
      return file;
    } catch (IOException ex) {
      throw new WingsException("Error in creating log file", ex);
    }
  }

  @Override
  public void pruneByActivity(String appId, String activityId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Log.class).field("appId").equal(appId).field("activityId").equal(activityId));
  }

  @Override
  public void batchedSave(List<Log> logs) {
    if (!isEmpty(logs)) {
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

      Map<String, Map<String, Log>> activityCommandUnitLastLogMap = logs.stream().collect(groupingBy(Log::getActivityId,
          toMap(Log::getCommandUnitName, Function.identity(),
              (l1, l2)
                  -> (l1.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)
                              || l1.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)
                          ? l1
                          : l2))));

      //      Map<String, Map<String, Log>> activityCommandUnitLastLogMap =
      //          logs.stream().collect(groupingBy(Log::getActivityId, toMap(Log::getCommandUnitName,
      //          Function.identity(), (l1, l2) -> l2)));
      activityService.updateCommandUnitStatus(activityCommandUnitLastLogMap);
    }
  }

  @Override
  public void purgeActivityLogs() {
    List<Key<Activity>> nonPurgedActivities =
        wingsPersistence.createQuery(Activity.class)
            .field("logPurged")
            .equal(false)
            .field("createdAt")
            .greaterThan(System.currentTimeMillis() - DataCleanUpJob.LOGS_RETENTION_TIME)
            .asKeyList();
    for (Key<Activity> activityKey : nonPurgedActivities) {
      List<Object> idsToKeep = new ArrayList<>();
      List<Key<Log>> logs = wingsPersistence.createQuery(Log.class)
                                .field("activityId")
                                .equal(activityKey.getId())
                                .order("-createdAt")
                                .asKeyList();

      for (Key<Log> logKey : logs) {
        idsToKeep.add(logKey.getId());
        if (idsToKeep.size() >= NUM_OF_LOGS_TO_KEEP) {
          break;
        }
      }

      wingsPersistence.delete(wingsPersistence.createQuery(Log.class)
                                  .field("activityId")
                                  .equal(activityKey.getId())
                                  .field("_id")
                                  .hasNoneOf(idsToKeep));
    }
  }

  @Override
  public String batchedSaveCommandUnitLogs(String activityId, String unitName, Log log) {
    logger.info("Batched log: [{}-{}-{}-{}]", log.getActivityId(), log.getCommandUnitName(), log.getLogLevel(),
        log.getCommandExecutionStatus());
    String logId = wingsPersistence.save(log);
    activityService.updateCommandUnitStatus(log.getAppId(), activityId, unitName, log.getCommandExecutionStatus());
    return logId;
  }
}
