package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.reflection.ReflectUtils.getEncryptedFields;
import static io.harness.reflection.ReflectUtils.getEncryptedRefField;
import static java.util.Collections.emptyList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
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

  @Inject private AppService appService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private AwsHelperService awsHelperService;
  @Inject private GcpHelperService gcpHelperService;
  @Inject private PcfHelperService pcfHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private NewRelicService newRelicService;
  @Inject private AnalysisService analysisService;
  @Inject private ElkAnalysisService elkAnalysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Transient private transient FeatureFlagService featureFlagService;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;
  @Inject private AwsEc2HelperServiceManager awsEc2HelperServiceManager;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private SettingsService settingsService;
  @Inject private ManagerDecryptionService managerDecryptionService;

  public boolean validate(SettingAttribute settingAttribute) {
    // Name has leading/trailing spaces
    if (wingsPersistence.createQuery(SettingAttribute.class)
            .filter("accountId", settingAttribute.getAccountId())
            .filter("appId", settingAttribute.getAppId())
            .filter("envId", settingAttribute.getEnvId())
            .field(Mapper.ID_KEY)
            .notEqual(settingAttribute.getUuid())
            .filter("name", settingAttribute.getName())
            .filter("category", settingAttribute.getCategory())
            .get()
        != null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args",
              "The name " + settingAttribute.getName() + " already exists in " + settingAttribute.getCategory() + ".");
    }

    SettingValue settingValue = settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = fetchEncryptionDetails(settingValue);

    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential((GcpConfig) settingValue, encryptedDataDetails);
    } else if (settingValue instanceof AzureConfig) {
      azureHelperService.validateAzureAccountCredential(((AzureConfig) settingValue).getClientId(),
          ((AzureConfig) settingValue).getTenantId(), new String(((AzureConfig) settingValue).getKey()));
    } else if (settingValue instanceof PcfConfig) {
      validatePcfConfig(settingAttribute, (PcfConfig) settingValue);
    } else if (settingValue instanceof AwsConfig) {
      validateAwsConfig(settingAttribute, encryptedDataDetails);
    } else if (settingValue instanceof KubernetesClusterConfig) {
      if (!((KubernetesClusterConfig) settingValue).isSkipValidation()) {
        validateKubernetesClusterConfig(settingAttribute);
      }
    } else if (settingValue instanceof JenkinsConfig || settingValue instanceof BambooConfig
        || settingValue instanceof NexusConfig || settingValue instanceof DockerConfig
        || settingValue instanceof ArtifactoryConfig) {
      buildSourceService.getBuildService(settingAttribute, Base.GLOBAL_APP_ID)
          .validateArtifactServer(settingValue, encryptedDataDetails);
    } else if (settingValue instanceof AppDynamicsConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.APP_DYNAMICS, encryptedDataDetails);
    } else if (settingValue instanceof DatadogConfig) {
      newRelicService.validateAPMConfig(
          settingAttribute, ((DatadogConfig) settingAttribute.getValue()).createAPMValidateCollectorConfig());
    } else if (settingValue instanceof BugsnagConfig) {
      newRelicService.validateAPMConfig(
          settingAttribute, ((BugsnagConfig) settingAttribute.getValue()).createAPMValidateCollectorConfig());
    } else if (settingValue instanceof APMVerificationConfig) {
      newRelicService.validateAPMConfig(settingAttribute,
          ((APMVerificationConfig) settingAttribute.getValue())
              .createAPMValidateCollectorConfig(secretManager, encryptionService));
      ((APMVerificationConfig) settingAttribute.getValue()).encryptFields(secretManager);
    } else if (settingValue instanceof SplunkConfig) {
      analysisService.validateConfig(settingAttribute, StateType.SPLUNKV2, encryptedDataDetails);
    } else if (settingValue instanceof ElkConfig) {
      if (((ElkConfig) settingValue).getElkConnector() == ElkConnector.KIBANA_SERVER) {
        try {
          ((ElkConfig) settingValue)
              .setKibanaVersion(elkAnalysisService.getVersion(
                  settingAttribute.getAccountId(), (ElkConfig) settingValue, Collections.emptyList()));
        } catch (Exception ex) {
          throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR, USER, ex).addParam("reason", Misc.getMessage(ex));
        }
      }
      analysisService.validateConfig(settingAttribute, StateType.ELK, encryptedDataDetails);
    } else if (settingValue instanceof LogzConfig) {
      analysisService.validateConfig(settingAttribute, StateType.LOGZ, encryptedDataDetails);
    } else if (settingValue instanceof SumoConfig) {
      analysisService.validateConfig(settingAttribute, StateType.SUMO, encryptedDataDetails);
    } else if (settingValue instanceof NewRelicConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.NEW_RELIC, encryptedDataDetails);
    } else if (settingValue instanceof DynaTraceConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.DYNA_TRACE, encryptedDataDetails);
    } else if (settingValue instanceof PrometheusConfig) {
      newRelicService.validateConfig(settingAttribute, StateType.PROMETHEUS, encryptedDataDetails);
    } else if (settingValue instanceof GitConfig) {
      validateGitConfig(settingAttribute, encryptedDataDetails);
    } else if (settingValue instanceof HostConnectionAttributes) {
      validateHostConnectionAttributes((HostConnectionAttributes) settingValue);
    }

    if (EncryptableSetting.class.isInstance(settingValue)) {
      EncryptableSetting encryptable = (EncryptableSetting) settingValue;
      List<Field> encryptedFields = getEncryptedFields(settingValue.getClass());
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

  private void validatePcfConfig(SettingAttribute settingAttribute, PcfConfig pcfConfig) {
    pcfHelperService.validate(pcfConfig);
  }

  private void validateKubernetesClusterConfig(SettingAttribute settingAttribute) {
    String namespace = "default";

    SyncTaskContext syncTaskContext = aContext().withAccountId(settingAttribute.getAccountId()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(emptyList())
                                                        .namespace(namespace)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateAwsConfig(SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      AwsConfig value = (AwsConfig) settingAttribute.getValue();
      awsEc2HelperServiceManager.validateAwsAccountCredential(value, encryptedDataDetails);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateGitConfig(SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
      gitConfig.setDecrypted(true);
      gitConfigHelperService.validateGitConfig(gitConfig, fetchEncryptionDetails(gitConfig));
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateHostConnectionAttributes(HostConnectionAttributes hostConnectionAttributes) {
    if (hostConnectionAttributes.getAuthenticationScheme() != null
        && hostConnectionAttributes.getAuthenticationScheme().equals(
               HostConnectionAttributes.AuthenticationScheme.SSH_KEY)) {
      if (hostConnectionAttributes.isKeyless()) {
        if (EmptyPredicate.isEmpty(hostConnectionAttributes.getKeyPath())) {
          throw new InvalidRequestException("Private key file path is not specified", USER);
        }
      } else {
        if (EmptyPredicate.isEmpty(hostConnectionAttributes.getKey())) {
          throw new InvalidRequestException("Private key is not specified", USER);
        }
      }
    }
  }

  private List<EncryptedDataDetail> fetchEncryptionDetails(SettingValue settingValue) {
    // Shouldn't we have accountId check while fetching the encrypted Data inside the below function
    if (EncryptableSetting.class.isInstance(settingValue)) {
      return secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);
    }

    return Collections.emptyList();
  }

  // For accountLevel Setting Attributes
  public SettingAttribute getAndDecryptSettingAttribute(String settingAttributeId) {
    SettingAttribute settingAttribute = settingsService.get(settingAttributeId);
    if (settingAttribute != null && settingAttribute.getValue() instanceof EncryptableSetting) {
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), GLOBAL_APP_ID, null);
      managerDecryptionService.decrypt((EncryptableSetting) settingAttribute.getValue(), encryptionDetails);
    }
    return settingAttribute;
  }
}
