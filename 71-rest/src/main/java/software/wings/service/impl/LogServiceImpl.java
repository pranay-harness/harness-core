package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
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
  public static final int NUM_OF_LOGS_TO_KEEP = 200;

  public static final String DATE_FORMATTER_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMATTER_PATTERN);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ActivityService activityService;
  @Inject private DataStoreService dataStoreService;
  @Inject private AppService appService;
  @Inject private FeatureFlagService featureFlagService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Log> list(String appId, PageRequest<Log> pageRequest) {
    return dataStoreService.list(Log.class, pageRequest);
  }

  @Override
  public File exportLogs(String appId, String activityId) {
    File file = new File(
        Files.createTempDir(), format("ActivityLogs_%s.txt", dateFormatter.format(new Date(currentTimeMillis()))));
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(file), UTF_8)) {
      List<Log> logList = dataStoreService
                              .list(Log.class,
                                  aPageRequest()
                                      .addFilter("appId", Operator.EQ, appId)
                                      .addFilter("activityId", Operator.EQ, activityId)
                                      .addOrder(Log.CREATED_AT_KEY, OrderType.ASC)
                                      .build())
                              .getResponse();
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
    dataStoreService.purgeByActivity(appId, activityId);
  }

  @Override
  public void batchedSave(List<Log> logs) {
    if (isNotEmpty(logs)) {
      logs = logs.stream().filter(Objects::nonNull).collect(toList());
      dataStoreService.save(Log.class, logs, false);

      // Map of [ActivityId -> [CommandUnitName -> LastLogLineStatus]]

      Map<String, Map<String, Log>> activityCommandUnitLastLogMap = logs.stream().collect(groupingBy(Log::getActivityId,
          toMap(Log::getCommandUnitName, Function.identity(),
              (l1, l2)
                  -> l1.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
                      || l1.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE
                  ? l1
                  : l2)));

      activityService.updateCommandUnitStatus(activityCommandUnitLastLogMap);
    }
  }

  @Override
  public boolean batchedSaveCommandUnitLogs(String activityId, String unitName, Log log) {
    dataStoreService.save(Log.class, Lists.newArrayList(log), false);
    activityService.updateCommandUnitStatus(log.getAppId(), activityId, unitName, log.getCommandExecutionStatus());
    return true;
  }
}
