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
import software.wings.service.impl.analysis.LogDataCollectionTaskResult;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.Misc;

import java.io.InputStream;
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
  private static final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final Logger logger = LoggerFactory.getLogger(SplunkDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<LogDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public LogDataCollectionTaskResult run(Object[] parameters) {
    final SplunkDataCollectionInfo dataCollectionInfo = (SplunkDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}" + dataCollectionInfo);

    final SplunkConfig splunkConfig = dataCollectionInfo.getSplunkConfig();
    final ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(splunkConfig.getUsername());
    loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));
    loginArgs.setHost(splunkConfig.getHost());
    loginArgs.setPort(splunkConfig.getPort());

    HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    Service splunkService = Service.connect(loginArgs);
    final ScheduledExecutorService collectionService = scheduleMetricDataCollection(dataCollectionInfo, splunkService);
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    scheduledExecutorService.schedule(() -> {
      try {
        logger.info("log collection finished for " + dataCollectionInfo);
        collectionService.shutdown();
        collectionService.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        collectionService.shutdown();
      }

      completed.set(true);
      synchronized (lockObject) {
        lockObject.notifyAll();
      }
    }, dataCollectionInfo.getCollectionTime() + DELAY_MINUTES + 1, TimeUnit.MINUTES);
    logger.info("going to collect splunk data for " + dataCollectionInfo);

    synchronized (lockObject) {
      while (!completed.get()) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return aLogDataCollectionTaskResult().withStatus(LogDataCollectionTaskStatus.SUCCESS).build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(
      SplunkDataCollectionInfo dataCollectionInfo, Service splunkService) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new SplunkDataCollector(dataCollectionInfo, splunkService, logAnalysisStoreService), DELAY_MINUTES, 1,
        TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private static class SplunkDataCollector implements Runnable {
    private final SplunkDataCollectionInfo dataCollectionInfo;
    private final Service splunkService;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute = 0;

    private SplunkDataCollector(SplunkDataCollectionInfo dataCollectionInfo, Service splunkService,
        LogAnalysisStoreService logAnalysisStoreService) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.splunkService = splunkService;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        return;
      }

      try {
        for (String query : dataCollectionInfo.getQueries()) {
          String hostStr = null;
          for (String host : dataCollectionInfo.getHosts()) {
            if (hostStr == null) {
              hostStr = "host = " + host;
            } else {
              hostStr += " OR "
                  + " host = " + host;
            }
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
          final List<LogElement> logElements = new ArrayList<>();
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
          logAnalysisStoreService.save(StateType.SPLUNKV2, dataCollectionInfo.getAccountId(),
              dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
              dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
              dataCollectionInfo.getServiceId(), logElements);
          logger.info("sent splunk search records to server. Num of events: " + job.getEventCount()
              + " application: " + dataCollectionInfo.getApplicationId()
              + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
      } catch (Exception e) {
        logger.error("error fetching splunk logs", e);
      }
    }
  }
}
