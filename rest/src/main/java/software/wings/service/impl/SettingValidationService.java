package software.wings.service.impl;

import static software.wings.beans.FeatureName.AZURE_SUPPORT;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.utils.WingsReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 5/1/17.
 */
@Singleton
public class SettingValidationService {
  private static final Logger logger = LoggerFactory.getLogger(SettingValidationService.class);

  @Inject private AwsHelperService awsHelperService;
  @Inject private GcpHelperService gcpHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private NewRelicService newRelicService;
  @Inject private AnalysisService analysisService;
  @Inject private ElkAnalysisService elkAnalysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Transient private transient FeatureFlagService featureFlagService;

  public boolean validate(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();

    if (wingsPersistence.createQuery(SettingAttribute.class)
            .field("accountId")
            .equal(settingAttribute.getAccountId())
            .field("appId")
            .equal(settingAttribute.getAppId())
            .field("envId")
            .equal(settingAttribute.getEnvId())
            .field(Mapper.ID_KEY)
            .notEqual(settingAttribute.getUuid())
            .field("name")
            .equal(settingAttribute.getName())
            .field("value.type")
            .equal(settingValue.getType())
            .get()
        != null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "The name " + settingAttribute.getName() + " already exists.");
    }

    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential((GcpConfig) settingValue);
    } else if (settingValue instanceof AzureConfig) {
      if (!featureFlagService.isEnabled(AZURE_SUPPORT, settingAttribute.getAccountId())) {
        throw new WingsException(ErrorCode.INVALID_REQUEST)
            .addParam("message", "Adding Azure as Cloud Provider is not supported yet.");
      }
      azureHelperService.validateAzureAccountCredential(((AzureConfig) settingValue).getClientId(),
          ((AzureConfig) settingValue).getTenantId(), ((AzureConfig) settingValue).getKey());
    } else if (settingValue instanceof AwsConfig) {
      awsHelperService.validateAwsAccountCredential(
          ((AwsConfig) settingValue).getAccessKey(), ((AwsConfig) settingValue).getSecretKey());
    } else if (settingValue instanceof JenkinsConfig || settingValue instanceof BambooConfig
        || settingValue instanceof NexusConfig || settingValue instanceof DockerConfig
        || settingValue instanceof ArtifactoryConfig) {
      buildSourceService.getBuildService(settingAttribute, Base.GLOBAL_APP_ID).validateArtifactServer(settingValue);
    } else if (settingValue instanceof AppDynamicsConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.APP_DYNAMICS);
    } else if (settingValue instanceof SplunkConfig) {
      analysisService.validateConfig(settingAttribute, StateType.SPLUNKV2);
    } else if (settingValue instanceof ElkConfig) {
      if (((ElkConfig) settingValue).getElkConnector() == ElkConnector.KIBANA_SERVER) {
        try {
          ((ElkConfig) settingValue)
              .setKibanaVersion(elkAnalysisService.getVersion(
                  settingAttribute.getAccountId(), (ElkConfig) settingValue, Collections.emptyList()));
        } catch (Exception ex) {
          logger.warn("Unable to validate ELK via Kibana", ex);
          return false;
        }
      }
      analysisService.validateConfig(settingAttribute, StateType.ELK);
    } else if (settingValue instanceof LogzConfig) {
      analysisService.validateConfig(settingAttribute, StateType.LOGZ);
    } else if (settingValue instanceof SumoConfig) {
      analysisService.validateConfig(settingAttribute, StateType.SUMO);
    } else if (settingValue instanceof NewRelicConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.NEW_RELIC);
    }

    if (Encryptable.class.isInstance(settingValue)) {
      Encryptable encryptable = (Encryptable) settingValue;
      List<Field> encryptedFields = WingsReflectionUtils.getEncryptedFields(settingValue.getClass());
      encryptedFields.forEach(encryptedField -> {
        Field encryptedFieldRef = getEncryptedRefField(encryptedField, encryptable);
        try {
          if (WingsReflectionUtils.isSetByYaml(encryptable, encryptedFieldRef)) {
            encryptedField.setAccessible(true);
            encryptedField.set(encryptable, null);
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    }

    return true;
  }
}
