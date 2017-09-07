package software.wings.service.impl.newrelic;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicServiceImpl implements NewRelicService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public void validateConfig(SettingAttribute settingAttribute) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .validateConfig((NewRelicConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.NEWRELIC_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
          .getAllApplications((NewRelicConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(
          ErrorCode.NEWRELIC_ERROR, "message", "Error in getting new relic applications. " + e.getMessage());
    }
  }

  @Override
  public boolean saveMetricData(String accountId, String applicationId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    logger.debug("inserting " + metricData.size() + " pieces of new relic metrics data");
    wingsPersistence.saveIgnoringDuplicateKeys(metricData);
    logger.debug("inserted " + metricData.size() + " NewRelicMetricDataRecord to persistence layer.");
    return true;
  }
}
