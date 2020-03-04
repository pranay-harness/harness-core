package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedFields;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.ccm.CCMSettingService;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnavailableFeatureException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InstanaConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.TaskType;
import software.wings.beans.ValidationResult;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationResponse;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl;
import software.wings.service.impl.spotinst.SpotinstHelperServiceManager;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DelegateService;
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
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.ConnectivityValidationDelegateResponse;
import software.wings.sm.StateType;
import software.wings.utils.WingsReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 5/1/17.
 */
@Singleton
@Slf4j
public class SettingValidationService {
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
  @Inject private JiraHelperService jiraHelperService;
  @Inject private DelegateService delegateService;
  @Inject private ServiceNowServiceImpl servicenowServiceImpl;
  @Inject private SpotinstHelperServiceManager spotinstHelperServiceManager;
  @Inject private CCMSettingService ccmSettingService;

  public ValidationResult validateConnectivity(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof HostConnectionAttributes || settingValue instanceof WinRmConnectionAttributes
        || settingValue instanceof SmtpConfig) {
      List<EncryptedDataDetail> encryptionDetails = null;
      encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      ConnectivityValidationDelegateRequest request = ConnectivityValidationDelegateRequest.builder()
                                                          .encryptedDataDetails(encryptionDetails)
                                                          .settingAttribute(settingAttribute)
                                                          .build();
      DelegateTask delegateTask = DelegateTask.builder()
                                      .accountId(settingAttribute.getAccountId())
                                      .appId(settingAttribute.getAppId())
                                      .async(false)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.CONNECTIVITY_VALIDATION.name())
                                                .parameters(new Object[] {request})
                                                .timeout(TimeUnit.MINUTES.toMillis(2))
                                                .build())
                                      .build();
      try {
        ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
        if (notifyResponseData instanceof ErrorNotifyResponseData) {
          return ValidationResult.builder()
              .errorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage())
              .valid(false)
              .build();
        } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
          return ValidationResult.builder()
              .errorMessage(
                  ExceptionUtils.getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()))
              .valid(false)
              .build();
        }
        if (!(notifyResponseData instanceof ConnectivityValidationDelegateResponse)) {
          throw new WingsException(ErrorCode.GENERAL_ERROR)
              .addParam("message", "Unknown Response from delegate")
              .addContext(ResponseData.class, notifyResponseData);
        } else {
          ConnectivityValidationDelegateResponse connectivityValidationDelegateResponse =
              (ConnectivityValidationDelegateResponse) notifyResponseData;
          return ValidationResult.builder()
              .errorMessage(connectivityValidationDelegateResponse.getErrorMessage())
              .valid(connectivityValidationDelegateResponse.isValid())
              .build();
        }
      } catch (InterruptedException ex) {
        throw new InvalidRequestException(ExceptionUtils.getMessage(ex), USER);
      }
    } else {
      try {
        return ValidationResult.builder().valid(validate(settingAttribute)).errorMessage("").build();
      } catch (Exception ex) {
        return ValidationResult.builder().valid(false).errorMessage(ExceptionUtils.getMessage(ex)).build();
      }
    }
  }

  public boolean validate(SettingAttribute settingAttribute) {
    // Name has leading/trailing spaces
    SettingAttribute sa = wingsPersistence.createQuery(SettingAttribute.class)
                              .filter(SettingAttributeKeys.accountId, settingAttribute.getAccountId())
                              .filter("appId", settingAttribute.getAppId())
                              .filter(SettingAttributeKeys.envId, settingAttribute.getEnvId())
                              .field(Mapper.ID_KEY)
                              .notEqual(settingAttribute.getUuid())
                              .filter(SettingAttributeKeys.name, settingAttribute.getName())
                              .filter(SettingAttributeKeys.category, settingAttribute.getCategory())
                              .get();
    if (sa != null) {
      throw new InvalidArgumentsException(
          "The name " + settingAttribute.getName() + " already exists in " + settingAttribute.getCategory() + ".",
          USER);
    }

    SettingValue settingValue = settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = fetchEncryptionDetails(settingValue);

    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential((GcpConfig) settingValue, encryptedDataDetails);
    } else if (settingValue instanceof AzureConfig) {
      azureHelperService.validateAzureAccountCredential((AzureConfig) settingValue, encryptedDataDetails);
    } else if (settingValue instanceof PcfConfig) {
      validatePcfConfig((PcfConfig) settingValue);
    } else if (settingValue instanceof AwsConfig) {
      validateAwsConfig(settingAttribute, encryptedDataDetails);
    } else if (settingValue instanceof KubernetesClusterConfig) {
      if (!((KubernetesClusterConfig) settingValue).isSkipValidation()) {
        boolean isCloudCostEnabled = ccmSettingService.isCloudCostEnabled(settingAttribute);
        validateKubernetesClusterConfig(settingAttribute, encryptedDataDetails, isCloudCostEnabled);
      }
    } else if (settingValue instanceof JenkinsConfig || settingValue instanceof BambooConfig
        || settingValue instanceof NexusConfig || settingValue instanceof DockerConfig
        || settingValue instanceof ArtifactoryConfig || settingValue instanceof SmbConfig
        || settingValue instanceof SftpConfig || settingValue instanceof AzureArtifactsConfig) {
      buildSourceService.getBuildService(settingAttribute, GLOBAL_APP_ID)
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
    } else if (settingValue instanceof InstanaConfig) {
      analysisService.validateConfig(settingAttribute, StateType.INSTANA, encryptedDataDetails);
    } else if (settingValue instanceof ElkConfig) {
      if (((ElkConfig) settingValue).getElkConnector() == ElkConnector.KIBANA_SERVER) {
        try {
          ((ElkConfig) settingValue)
              .setKibanaVersion(elkAnalysisService.getVersion(
                  settingAttribute.getAccountId(), (ElkConfig) settingValue, Collections.emptyList()));
        } catch (Exception ex) {
          throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR, USER, ex)
              .addParam("reason", ExceptionUtils.getMessage(ex));
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
    } else if (settingValue instanceof JiraConfig) {
      jiraHelperService.validateCredential((JiraConfig) settingValue);
    } else if (settingValue instanceof ServiceNowConfig) {
      servicenowServiceImpl.validateCredential(settingAttribute);
    } else if (settingValue instanceof HelmRepoConfig) {
      validateHelmRepoConfig(settingAttribute, encryptedDataDetails);
    } else if (settingValue instanceof SpotInstConfig) {
      if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, settingAttribute.getAccountId())) {
        throw new UnavailableFeatureException(
            "Enable Feature Flag INFRA_MAPPING_REFACTOR to create Spotinst Cloud Provider", USER);
      }
      validateSpotInstConfig(settingAttribute, encryptedDataDetails);
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

  private void validateSpotInstConfig(
      SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      spotinstHelperServiceManager.listElastigroups(
          (SpotInstConfig) settingAttribute.getValue(), encryptedDataDetails, GLOBAL_APP_ID);
    } catch (Exception ex) {
      throw new InvalidRequestException("Invalid spotinst credentials");
    }
  }

  private void validatePcfConfig(PcfConfig pcfConfig) {
    pcfHelperService.validate(pcfConfig);
  }

  private boolean validateKubernetesClusterConfig(
      SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails, boolean isCloudCostEnabled) {
    SettingValue settingValue = settingAttribute.getValue();
    Preconditions.checkArgument(((KubernetesClusterConfig) settingValue).isSkipValidation() == false);

    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(settingAttribute.getAccountId()).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    ContainerService containerService = delegateProxyFactory.get(ContainerService.class, syncTaskContext);

    String namespace = "default";
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptedDataDetails)
                                                        .namespace(namespace)
                                                        .cloudCostEnabled(isCloudCostEnabled)
                                                        .build();
    try {
      return containerService.validate(containerServiceParams);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  private void validateAwsConfig(SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    AwsConfig value = (AwsConfig) settingAttribute.getValue();
    try {
      awsEc2HelperServiceManager.validateAwsAccountCredential(value, encryptedDataDetails);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  private void validateGitConfig(SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
      gitConfig.setDecrypted(true);
      gitConfigHelperService.validateGitConfig(gitConfig, encryptedDataDetails);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  private void validateHostConnectionAttributes(HostConnectionAttributes hostConnectionAttributes) {
    if (hostConnectionAttributes.getAuthenticationScheme() != null
        && hostConnectionAttributes.getAuthenticationScheme()
            == HostConnectionAttributes.AuthenticationScheme.SSH_KEY) {
      if (isEmpty(hostConnectionAttributes.getUserName())) {
        throw new InvalidRequestException("Username field is mandatory in SSH Configuration", USER);
      }
      if (hostConnectionAttributes.isKeyless()) {
        if (isEmpty(hostConnectionAttributes.getKeyPath())) {
          throw new InvalidRequestException("Private key file path is not specified", USER);
        }
      } else {
        if (hostConnectionAttributes.getAccessType() != null
            && hostConnectionAttributes.getAccessType() == HostConnectionAttributes.AccessType.USER_PASSWORD) {
          if (isEmpty(hostConnectionAttributes.getSshPassword())) {
            throw new InvalidRequestException("Password field is mandatory in SSH Configuration", USER);
          }
        } else if (isEmpty(hostConnectionAttributes.getKey())) {
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

  private HelmRepoConfigValidationTaskParams getHelmRepoConfigValidationTaskParams(
      SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    HelmRepoConfigValidationTaskParams taskParams = HelmRepoConfigValidationTaskParams.builder()
                                                        .accountId(settingAttribute.getAccountId())
                                                        .appId(settingAttribute.getAppId())
                                                        .encryptedDataDetails(encryptedDataDetails)
                                                        .helmRepoConfig((HelmRepoConfig) settingAttribute.getValue())
                                                        .repoDisplayName(settingAttribute.getName())
                                                        .build();

    String connectorId = ((HelmRepoConfig) settingAttribute.getValue()).getConnectorId();
    if (isNotBlank(connectorId)) {
      SettingAttribute connectorSettingAttribute = settingsService.get(settingAttribute.getAppId(), connectorId);
      notNullCheck("Connector doesn't exist for id " + connectorId, connectorSettingAttribute);
      List<EncryptedDataDetail> connectorEncryptedDataDetails =
          fetchEncryptionDetails(connectorSettingAttribute.getValue());

      taskParams.setConnectorConfig(connectorSettingAttribute.getValue());
      taskParams.setConnectorEncryptedDataDetails(connectorEncryptedDataDetails);
    }

    return taskParams;
  }

  private String getTrimmedValue(String value) {
    return (value == null) ? null : value.trim();
  }

  private void validateAndSanitizeHelmRepoConfigValues(SettingAttribute settingAttribute) {
    HelmRepoConfig helmRepoConfig = (HelmRepoConfig) settingAttribute.getValue();

    switch (helmRepoConfig.getSettingType()) {
      case HTTP_HELM_REPO:
        HttpHelmRepoConfig httpHelmRepoConfig = (HttpHelmRepoConfig) helmRepoConfig;
        if (isBlank(httpHelmRepoConfig.getChartRepoUrl())) {
          throw new InvalidRequestException("Helm repository URL cannot be empty", USER);
        }

        httpHelmRepoConfig.setChartRepoUrl(getTrimmedValue(httpHelmRepoConfig.getChartRepoUrl()));
        httpHelmRepoConfig.setUsername(getTrimmedValue(httpHelmRepoConfig.getUsername()));

        break;
      case AMAZON_S3_HELM_REPO:
        AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
        if (isBlank(amazonS3HelmRepoConfig.getConnectorId())) {
          throw new InvalidRequestException("Cloud provider cannot be empty", USER);
        }

        if (isBlank(amazonS3HelmRepoConfig.getBucketName())) {
          throw new InvalidRequestException("S3 bucket cannot be empty", USER);
        }

        if (isBlank(amazonS3HelmRepoConfig.getRegion())) {
          throw new InvalidRequestException("Region cannot be empty", USER);
        }

        amazonS3HelmRepoConfig.setConnectorId(getTrimmedValue(amazonS3HelmRepoConfig.getConnectorId()));
        amazonS3HelmRepoConfig.setBucketName(getTrimmedValue(amazonS3HelmRepoConfig.getBucketName()));
        amazonS3HelmRepoConfig.setRegion(getTrimmedValue(amazonS3HelmRepoConfig.getRegion()));

        break;
      case GCS_HELM_REPO:
        break;
      default:
        unhandled(helmRepoConfig.getSettingType());
        throw new UnknownEnumTypeException("helm repository type",
            helmRepoConfig.getSettingType() == null ? "null" : helmRepoConfig.getSettingType().name());
    }
  }

  private void validateHelmRepoConfig(
      SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    validateAndSanitizeHelmRepoConfigValues(settingAttribute);

    HelmRepoConfigValidationResponse repoConfigValidationResponse;

    try {
      HelmRepoConfigValidationTaskParams taskParams =
          getHelmRepoConfigValidationTaskParams(settingAttribute, encryptedDataDetails);

      DelegateTask delegateTask = DelegateTask.builder()
                                      .accountId(settingAttribute.getAccountId())
                                      .appId(settingAttribute.getAppId())
                                      .async(false)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.HELM_REPO_CONFIG_VALIDATION.name())
                                                .parameters(new Object[] {taskParams})
                                                .timeout(TimeUnit.MINUTES.toMillis(2))
                                                .build())
                                      .build();

      ResponseData notifyResponseData = delegateService.executeTask(delegateTask);

      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      } else if ((notifyResponseData instanceof RemoteMethodReturnValueData)
          && (((RemoteMethodReturnValueData) notifyResponseData).getException() instanceof InvalidRequestException)) {
        throw(InvalidRequestException)((RemoteMethodReturnValueData) notifyResponseData).getException();
      } else if (!(notifyResponseData instanceof HelmRepoConfigValidationResponse)) {
        throw new WingsException(ErrorCode.GENERAL_ERROR)
            .addParam("message", "Unknown response from delegate")
            .addContext(ResponseData.class, notifyResponseData);
      }

      repoConfigValidationResponse = (HelmRepoConfigValidationResponse) notifyResponseData;
    } catch (InterruptedException e) {
      repoConfigValidationResponse = HelmRepoConfigValidationResponse.builder()
                                         .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                         .errorMessage(ExceptionUtils.getMessage(e))
                                         .build();
    }

    if (CommandExecutionStatus.FAILURE == repoConfigValidationResponse.getCommandExecutionStatus()) {
      logger.warn(repoConfigValidationResponse.getErrorMessage());
      throw new InvalidRequestException(repoConfigValidationResponse.getErrorMessage());
    }
  }
}
