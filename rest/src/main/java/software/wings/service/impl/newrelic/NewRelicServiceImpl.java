package software.wings.service.impl.newrelic;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.APMFetchConfig;
import software.wings.annotation.Encryptable;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
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
import software.wings.service.impl.newrelic.NewRelicApplication.NewRelicApplications;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;
import software.wings.utils.CacheHelper;
import software.wings.utils.Misc;

import java.util.List;
import javax.cache.Cache;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicServiceImpl implements NewRelicService {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private CacheHelper cacheHelper;

  @Override
  public void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config) {
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).validateCollector(config);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR).addParam("reason", errorMsg);
    }
  }

  @Override
  public String fetch(String accountId, String serverConfigId, APMFetchConfig fetchConfig) {
    try {
      SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
      APMValidateCollectorConfig apmValidateCollectorConfig =
          APMValidateCollectorConfig.builder()
              .baseUrl(apmVerificationConfig.getUrl())
              .headers(apmVerificationConfig.collectionHeaders())
              .options(apmVerificationConfig.collectionParams())
              .url(fetchConfig.getUrl())
              .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
              .build();
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
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
      throw new WingsException(errorCode).addParam("reason", Misc.getMessage(e));
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
          Cache<String, NewRelicApplications> newRelicApplicationCache = cacheHelper.getNewRelicApplicationCache();
          String key = settingAttribute.getUuid();
          NewRelicApplications applications;
          try {
            applications = newRelicApplicationCache.get(key);
            if (applications != null) {
              return applications.getApplications();
            }
          } catch (Exception ex) {
            // If there was any exception, remove that entry from cache
            newRelicApplicationCache.remove(key);
          }

          errorCode = ErrorCode.NEWRELIC_ERROR;
          List<NewRelicApplication> allApplications =
              delegateProxyFactory.get(NewRelicDelegateService.class, syncTaskContext)
                  .getAllApplications((NewRelicConfig) settingAttribute.getValue(), encryptionDetails, null);
          applications = NewRelicApplications.builder().applications(allApplications).build();
          newRelicApplicationCache.put(key, applications);
          return allApplications;
        case APP_DYNAMICS:
          errorCode = ErrorCode.APPDYNAMICS_ERROR;
          return delegateProxyFactory.get(AppdynamicsDelegateService.class, syncTaskContext)
              .getAllApplications((AppDynamicsConfig) settingAttribute.getValue(), encryptionDetails);
        default:
          throw new IllegalStateException("Invalid state" + stateType);
      }

    } catch (Exception e) {
      throw new WingsException(errorCode).addParam(
          "message", "Error in getting new relic applications. " + Misc.getMessage(e));
    }
  }
}
