package software.wings.service.impl.newrelic;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicServiceImpl implements NewRelicService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;

  @Override
  public void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).validateCollector(config);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR).addParam("reason", errorMsg);
    }
  }

  @Override
  public void validateConfig(SettingAttribute settingAttribute, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      switch (stateType) {
        case NEW_RELIC:
          errorCode = ErrorCode.NEWRELIC_CONFIGURATION_ERROR;
          delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
              .validateConfig((NewRelicConfig) settingAttribute.getValue());
          break;
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR;
          AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
          delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext).validateConfig(appDynamicsConfig);
          break;
        case DYNA_TRACE:
          errorCode = ErrorCode.DYNA_TRACE_CONFIGURATION_ERROR;
          DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingAttribute.getValue();
          delegateProxyFactory.get(DynaTraceDelegateService.class, syncTaskContext).validateConfig(dynaTraceConfig);
          break;
        case PROMETHEUS:
          errorCode = ErrorCode.PROMETHEUS_CONFIGURATION_ERROR;
          PrometheusConfig prometheusConfig = (PrometheusConfig) settingAttribute.getValue();
          delegateProxyFactory.get(PrometheusDelegateService.class, syncTaskContext).validateConfig(prometheusConfig);
          break;
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode).addParam("reason", e.getMessage());
    }
  }

  @Override
  public List<NewRelicApplication> getApplications(String settingId, StateType stateType) {
    ErrorCode errorCode = null;
    try {
      final SettingAttribute settingAttribute = settingsService.get(settingId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      switch (stateType) {
        case NEW_RELIC:
          errorCode = ErrorCode.NEWRELIC_ERROR;
          return delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
              .getAllApplications((NewRelicConfig) settingAttribute.getValue(), encryptionDetails);
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_ERROR;
          return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
              .getAllApplications((AppDynamicsConfig) settingAttribute.getValue(), encryptionDetails);
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }

    } catch (Exception e) {
      throw new WingsException(errorCode).addParam(
          "message", "Error in getting new relic applications. " + e.getMessage());
    }
  }
}
