package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.Constants.URL_STRING;

import com.google.inject.Inject;

import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  public static final int DELAY_MINUTES = 2;
  public static final Duration RETRY_SLEEP = Duration.ofSeconds(30);

  private static final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final Logger logger = LoggerFactory.getLogger(SplunkDataCollectionTask.class);
  private Service splunkService;
  private SplunkDataCollectionInfo dataCollectionInfo;

  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private DelegateLogService delegateLogService;

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    DataCollectionTaskResult taskResult = DataCollectionTaskResult.builder()
                                              .status(DataCollectionTaskStatus.SUCCESS)
                                              .stateType(StateType.SPLUNKV2)
                                              .build();
    this.dataCollectionInfo = (SplunkDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}", dataCollectionInfo);

    final SplunkConfig splunkConfig = dataCollectionInfo.getSplunkConfig();
    encryptionService.decrypt(splunkConfig, dataCollectionInfo.getEncryptedDataDetails());
    final ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(splunkConfig.getUsername());
    loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));

    URI uri;
    try {
      uri = new URI(splunkConfig.getSplunkUrl().trim());
    } catch (Exception ex) {
      taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage("Invalid server URL " + splunkConfig.getSplunkUrl());
      return taskResult;
    }

    loginArgs.setHost(uri.getHost());
    loginArgs.setPort(uri.getPort());

    if (uri.getScheme().equals("https")) {
      HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    }

    try {
      splunkService = new Service(loginArgs);
      splunkService.setConnectTimeout(HTTP_TIMEOUT);
      splunkService.setReadTimeout(HTTP_TIMEOUT);
      splunkService = Service.connect(loginArgs);
    } catch (Exception ex) {
      taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage("Unable to connect to server : " + Misc.getMessage(ex));
      return taskResult;
    }
    return taskResult;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new SplunkDataCollector(getTaskId(), dataCollectionInfo, splunkService, logAnalysisStoreService, taskResult);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected StateType getStateType() {
    return StateType.SPLUNKV2;
  }

  private class SplunkDataCollector implements Runnable {
    private String delegateTaskId;
    private final SplunkDataCollectionInfo dataCollectionInfo;
    private final Service splunkService;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private SplunkDataCollector(String delegateTaskId, SplunkDataCollectionInfo dataCollectionInfo,
        Service splunkService, LogAnalysisStoreService logAnalysisStoreService, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.splunkService = splunkService;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            final List<LogElement> logElements = new ArrayList<>();
            final List<Callable<List<LogElement>>> callables = new ArrayList<>();
            for (String host : dataCollectionInfo.getHosts()) {
              String query = dataCollectionInfo.getQuery();
              /* Heart beat */
              final LogElement splunkHeartBeatElement = new LogElement();
              splunkHeartBeatElement.setQuery(query);
              splunkHeartBeatElement.setClusterLabel("-3");
              splunkHeartBeatElement.setHost(host);
              splunkHeartBeatElement.setCount(0);
              splunkHeartBeatElement.setLogMessage("");
              splunkHeartBeatElement.setTimeStamp(0);
              splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);
              logElements.add(splunkHeartBeatElement);
              callables.add(() -> fetchLogsForHost(host, query));
            }

            List<Optional<List<LogElement>>> results = executeParrallel(callables);
            results.forEach(result -> {
              if (result.isPresent()) {
                logElements.addAll(result.get());
              }
            });

            boolean response = logAnalysisStoreService.save(StateType.SPLUNKV2, dataCollectionInfo.getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), delegateTaskId, logElements);
            if (!response) {
              if (++retry == RETRIES) {
                taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                // TODO capture error code and send back for all collectors
                taskResult.setErrorMessage("Cannot save log records. Server returned error ");
                completed.set(true);
                break;
              }
              continue;
            }
            logger.info("sent splunk search records to server. Num of events: " + logElements.size()
                + " application: " + dataCollectionInfo.getApplicationId()
                + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);
            break;
          } catch (Exception e) {
            if (++retry == RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              throw e;
            } else {
              /*
               * Save the exception from the first attempt. This is usually
               * more meaningful to trouble shoot.
               */
              if (retry == 1) {
                taskResult.setErrorMessage(Misc.getMessage(e));
              }
              logger.warn("error fetching splunk logs. retrying in " + RETRY_SLEEP + "s", e);
              sleep(RETRY_SLEEP);
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        completed.set(true);
        if (taskResult.getStatus() != DataCollectionTaskStatus.FAILURE) {
          taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
          taskResult.setErrorMessage("error fetching splunk logs for minute " + logCollectionMinute);
        }
        logger.error("error fetching splunk logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down Splunk data collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
        return;
      }
    }

    private List<LogElement> fetchLogsForHost(String host, String query) throws IOException, ParseException {
      final List<LogElement> logElements = new ArrayList<>();

      final String searchQuery = "search " + query + " " + dataCollectionInfo.getHostnameField() + " = " + host
          + " | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t"
          + "| table _time, _raw,cluster_label, host | "
          + "stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,host";

      JobArgs jobargs = new JobArgs();
      jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

      jobargs.setEarliestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(collectionStartTime)));
      final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(1) - 1;
      jobargs.setLatestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(endTime)));

      // A blocking search returns the job when the search is done
      ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
      apiCallLog.setTitle("Fetch request to " + dataCollectionInfo.getSplunkConfig().getSplunkUrl());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name(URL_STRING)
                                       .value(dataCollectionInfo.getSplunkConfig().getSplunkUrl())
                                       .type(FieldType.URL)
                                       .build());
      apiCallLog.addFieldToRequest(
          ThirdPartyApiCallField.builder().name("Query").value(searchQuery).type(FieldType.TEXT).build());
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      logger.info("triggering splunk query startTime: " + collectionStartTime + " endTime: " + endTime
          + " query: " + searchQuery + " url: " + dataCollectionInfo.getSplunkConfig().getSplunkUrl());
      Job job = splunkService.getJobs().create(searchQuery, jobargs);
      logger.info("splunk query done. Num of events: " + job.getEventCount() + " application: "
          + dataCollectionInfo.getApplicationId() + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId());

      JobResultsArgs resultsArgs = new JobResultsArgs();
      resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

      InputStream results = job.getResults(resultsArgs);
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(200, "splunk query done. Num of events: " + job.getEventCount(), FieldType.TEXT);
      delegateLogService.save(getAccountId(), apiCallLog);
      ResultsReaderJson resultsReader = new ResultsReaderJson(results);
      Map<String, String> event;

      while ((event = resultsReader.getNextEvent()) != null) {
        final LogElement splunkLogElement = new LogElement();
        splunkLogElement.setQuery(query);
        splunkLogElement.setClusterLabel(event.get("cluster_label"));
        splunkLogElement.setHost(host);
        splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
        splunkLogElement.setLogMessage(event.get("_raw"));
        splunkLogElement.setTimeStamp(SPLUNK_DATE_FORMATER.parse(event.get("_time")).getTime());
        splunkLogElement.setLogCollectionMinute(logCollectionMinute);
        logElements.add(splunkLogElement);
      }

      resultsReader.close();

      logger.info("for host {} got records {}", host, logElements.size());
      return logElements;
    }
  }
}
