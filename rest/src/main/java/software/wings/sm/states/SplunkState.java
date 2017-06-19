package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

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
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkSettingProvider;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    return null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    return super.handleAsyncResponse(context, response);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void triggerSplunkDataCollection(ExecutionContext context) {
    final SettingAttribute settingAttribute = settingsService.get(splunkConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No appdynamics setting with id: " + splunkConfigId + " found");
    }

    final SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();
    final List<String> queries = Arrays.asList(query.split(","));
    final SplunkDataCollectionInfo dataCollectionInfo =
        new SplunkDataCollectionInfo(splunkConfig, queries, Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.SPLUNK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .build();
    waitNotifyEngine.waitForAll(new AppdynamicsMetricDataCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }
}
