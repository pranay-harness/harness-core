package software.wings.service.impl;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.wings.beans.CommandUnit.ExecutionResult.RUNNING;
import static software.wings.utils.FileUtils.createTempDirPath;

import com.google.inject.Inject;

import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class LogServiceImpl implements LogService {
  private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  @Inject private WingsPersistence wingsPersistence;

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
  public Log save(Log log) {
    return wingsPersistence.saveAndGet(Log.class, log);
  }

  @Override
  public ExecutionResult getUnitExecutionResult(String appId, String activityId, String name) {
    Log log = wingsPersistence.createQuery(Log.class)
                  .field("activityId")
                  .equal(activityId)
                  .field("appId")
                  .equal(appId)
                  .field("commandUnitName")
                  .equal(name)
                  .order("-lastUpdatedAt")
                  .get();
    return log != null && log.getExecutionResult() != null ? log.getExecutionResult() : RUNNING;
  }

  @Override
  public File exportLogs(String appId, String activityId) {
    File file = new File(
        createTempDirPath(), format("ActivityLogs_%s.txt", dateFormatter.format(new Date(currentTimeMillis()))));
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
}
