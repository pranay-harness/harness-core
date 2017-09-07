package software.wings.delegatetasks;

import static software.wings.service.impl.analysis.LogDataCollectionTaskResult.Builder.aLogDataCollectionTaskResult;

import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.SplunkConfig;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkDataCollectionTask extends AbstractDelegateRunnableTask<LogDataCollectionTaskResult> {
  public static final int DELAY_MINUTES = 2;
  private static final int RETRIES = 3;
  private static final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final Logger logger = LoggerFactory.getLogger(SplunkDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private ScheduledExecutorService collectionService;

  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<LogDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public LogDataCollectionTaskResult run(Object[] parameters) {
    LogDataCollectionTaskResult taskResult =
        aLogDataCollectionTaskResult().withStatus(LogDataCollectionTaskStatus.SUCCESS).build();
    try {
      final SplunkDataCollectionInfo dataCollectionInfo = (SplunkDataCollectionInfo) parameters[0];
      logger.info("log collection - dataCollectionInfo: {}" + dataCollectionInfo);

      final SplunkConfig splunkConfig = dataCollectionInfo.getSplunkConfig();
      final ServiceArgs loginArgs = new ServiceArgs();
      loginArgs.setUsername(splunkConfig.getUsername());
      loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));

      URI uri;
      try {
        uri = new URI(splunkConfig.getSplunkUrl().trim());
      } catch (Exception ex) {
        taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("Invalid server URL " + splunkConfig.getSplunkUrl());
        return taskResult;
      }

      loginArgs.setHost(uri.getHost());
      loginArgs.setPort(uri.getPort());

      if (uri.getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }

      Service splunkService;
      try {
        splunkService = Service.connect(loginArgs);
      } catch (Exception ex) {
        taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("Unable to connect to server : " + ex.getMessage());
        return taskResult;
      }

      collectionService = scheduleMetricDataCollection(dataCollectionInfo, splunkService, taskResult);
      logger.info("going to collect splunk data for " + dataCollectionInfo);

      synchronized (lockObject) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          completed.set(true);
          logger.info("Splunk data collection interrupted");
        }
      }
      return taskResult;
    } catch (Exception e) {
      taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage(e.getMessage());
    }

    return taskResult;
  }

  private void shutDownCollection() {
    /* Redundant now, but useful if calling shutDownCollection
     * from the worker threads before the job is aborted
     */
    completed.set(true);
    collectionService.shutdownNow();
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  private ScheduledExecutorService scheduleMetricDataCollection(
      SplunkDataCollectionInfo dataCollectionInfo, Service splunkService, LogDataCollectionTaskResult taskResult) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new SplunkDataCollector(getTaskId(), dataCollectionInfo, splunkService, logAnalysisStoreService, taskResult),
        DELAY_MINUTES, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private class SplunkDataCollector implements Runnable {
    private String delegateTaskId;
    private final SplunkDataCollectionInfo dataCollectionInfo;
    private final Service splunkService;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute = 0;
    private LogDataCollectionTaskResult taskResult;

    private SplunkDataCollector(String delegateTaskId, SplunkDataCollectionInfo dataCollectionInfo,
        Service splunkService, LogAnalysisStoreService logAnalysisStoreService,
        LogDataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.splunkService = splunkService;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public void run() {
      try {
        for (String query : dataCollectionInfo.getQueries()) {
          int retry = 0;
          while (!completed.get() && retry < RETRIES) {
            try {
              String hostStr = null;
              final List<LogElement> logElements = new ArrayList<>();
              for (String host : dataCollectionInfo.getHosts()) {
                if (hostStr == null) {
                  hostStr = "host = " + host;
                } else {
                  hostStr += " OR "
                      + " host = " + host;
                }

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
              }

              if (hostStr == null) {
                throw new IllegalArgumentException("No hosts found for SplunkV2Task " + dataCollectionInfo.toString());
              }

              hostStr = " (" + hostStr + ") ";

              final String searchQuery = "search " + query + hostStr
                  + " | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t"
                  + "| table _time, _raw,cluster_label, host | "
                  + "stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,host";

              JobArgs jobargs = new JobArgs();
              jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

              jobargs.setEarliestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(collectionStartTime)));
              final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(1) - 1;
              jobargs.setLatestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(endTime)));

              // A blocking search returns the job when the search is done
              logger.info("triggering splunk query startTime: " + collectionStartTime + " endTime: " + endTime
                  + " query: " + searchQuery);
              Job job = splunkService.getJobs().create(searchQuery, jobargs);
              logger.info("splunk query done. Num of events: " + job.getEventCount()
                  + " application: " + dataCollectionInfo.getApplicationId()
                  + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId());

              JobResultsArgs resultsArgs = new JobResultsArgs();
              resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

              InputStream results = job.getResults(resultsArgs);
              ResultsReaderJson resultsReader = new ResultsReaderJson(results);
              HashMap<String, String> event;

              while ((event = resultsReader.getNextEvent()) != null) {
                final LogElement splunkLogElement = new LogElement();
                splunkLogElement.setQuery(query);
                splunkLogElement.setClusterLabel(event.get("cluster_label"));
                splunkLogElement.setHost(event.get("host"));
                splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
                splunkLogElement.setLogMessage(event.get("_raw"));
                splunkLogElement.setTimeStamp(SPLUNK_DATE_FORMATER.parse(event.get("_time")).getTime());
                splunkLogElement.setLogCollectionMinute(logCollectionMinute);
                logElements.add(splunkLogElement);
              }

              resultsReader.close();
              boolean response = logAnalysisStoreService.save(StateType.SPLUNKV2, dataCollectionInfo.getAccountId(),
                  dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                  dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                  dataCollectionInfo.getServiceId(), delegateTaskId, logElements);
              if (!response) {
                if (++retry == RETRIES) {
                  taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
                  taskResult.setErrorMessage("Cannot save log records. Server returned error");
                  completed.set(true);
                  break;
                }
                continue;
              }
              logger.info("sent splunk search records to server. Num of events: " + job.getEventCount()
                  + " application: " + dataCollectionInfo.getApplicationId() + " stateExecutionId: "
                  + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);
              break;
            } catch (Exception e) {
              if (++retry == RETRIES) {
                taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
                taskResult.setErrorMessage(e.getMessage());
                completed.set(true);
                throw(e);
              } else {
                logger.warn("error fetching elk logs. retrying...", e);
              }
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        logger.error("error fetching splunk logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down Splunk data collection");
        shutDownCollection();
        return;
      }
    }
  }
}
