package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.service.impl.splunk.SplunkAnalysisResponse.Builder.anSplunkAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.collect.AppdynamicsMetricDataCallback;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.splunk.SplunkAnalysisResponse;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkExecutionData;
import software.wings.service.impl.splunk.SplunkLogCollectionCallback;
import software.wings.service.impl.splunk.SplunkSettingProvider;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class SplunkState extends State {
  private static final Logger logger = LoggerFactory.getLogger(SplunkState.class);

  @EnumData(enumDataProvider = SplunkSettingProvider.class)
  @Attributes(required = true, title = "Splunk Server")
  private String splunkConfigId;

  @Attributes(required = true, title = "Query") private String query;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  private String timeDuration;

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private SettingsService settingsService;

  @Transient @Inject private AppService appService;

  @Transient @Inject private DelegateService delegateService;

  public SplunkState(String name) {
    super(name, StateType.SPLUNK.getType());
  }

  /**
   * Getter for property 'query'.
   *
   * @return Value for property 'query'.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Setter for property 'query'.
   *
   * @param query Value to set for property 'query'.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  public String getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public String getSplunkConfigId() {
    return splunkConfigId;
  }

  public void setSplunkConfigId(String splunkConfigId) {
    this.splunkConfigId = splunkConfigId;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing splunk state");
    triggerSplunkDataCollection(context);
    final SplunkExecutionData executionData = SplunkExecutionData.Builder.anSplunkExecutionData()
                                                  .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                  .withSplunkConfigID(splunkConfigId)
                                                  .withSplunkQueries(Lists.newArrayList(query.split(",")))
                                                  .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                  .withCorrelationId(UUID.randomUUID().toString())
                                                  .build();
    final SplunkAnalysisResponse response = anSplunkAnalysisResponse()
                                                .withSplunkExecutionData(executionData)
                                                .withExecutionStatus(ExecutionStatus.SUCCESS)
                                                .build();
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      waitNotifyEngine.notify(executionData.getCorrelationId(), response);
    }, Long.parseLong(timeDuration), TimeUnit.MINUTES);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withErrorMessage("Splunk Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    SplunkAnalysisResponse executionResponse = (SplunkAnalysisResponse) response.values().iterator().next();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withStateExecutionData(executionResponse.getSplunkExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void triggerSplunkDataCollection(ExecutionContext context) {
    final SettingAttribute settingAttribute = settingsService.get(splunkConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No splunk setting with id: " + splunkConfigId + " found");
    }

    final SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();
    final List<String> queries = Lists.newArrayList(query.split(","));
    final SplunkDataCollectionInfo dataCollectionInfo =
        new SplunkDataCollectionInfo(appService.get(context.getAppId()).getAccountId(), context.getAppId(),
            splunkConfig, queries, Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.SPLUNK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .build();
    waitNotifyEngine.waitForAll(new SplunkLogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }
}
