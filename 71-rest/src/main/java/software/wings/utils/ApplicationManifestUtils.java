package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.manifest.ManifestHelper.getMapFromValuesFileContent;
import static io.harness.k8s.manifest.ManifestHelper.getValuesExpressionKeysFromMap;
import static io.harness.k8s.manifest.ManifestHelper.getValuesYamlGitFilePath;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ApplicationManifestUtils {
  @Inject private AppService appService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private GitFileConfigHelperService gitFileConfigHelperService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private FeatureFlagService featureFlagService;

  public Map<K8sValuesLocation, ApplicationManifest> getOverrideApplicationManifests(
      ExecutionContext context, AppManifestKind appManifestKind) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(context.getAppId(), serviceElement.getUuid(), appManifestKind);
    if (applicationManifest != null) {
      appManifestMap.put(K8sValuesLocation.ServiceOverride, applicationManifest);
    }

    InfrastructureMapping infraMapping = infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
    if (infraMapping == null) {
      throw new InvalidRequestException(format(
          "Infra mapping not found for appId %s infraMappingId %s", app.getUuid(), context.fetchInfraMappingId()));
    }

    applicationManifest =
        applicationManifestService.getByEnvId(app.getUuid(), infraMapping.getEnvId(), appManifestKind);
    if (applicationManifest != null) {
      appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, applicationManifest);
    }

    applicationManifest = applicationManifestService.getByEnvAndServiceId(
        app.getUuid(), infraMapping.getEnvId(), serviceElement.getUuid(), appManifestKind);
    if (applicationManifest != null) {
      appManifestMap.put(K8sValuesLocation.Environment, applicationManifest);
    }

    return appManifestMap;
  }

  public GitFetchFilesTaskParams createGitFetchFilesTaskParams(
      ExecutionContext context, Application app, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = getGitFetchFileConfigMap(context, app, appManifestMap);

    ContainerServiceParams containerServiceParams = null;
    String infrastructureMappingId = context.fetchInfraMappingId();

    if (infrastructureMappingId != null) {
      InfrastructureMapping infraMapping =
          infrastructureMappingService.get(context.getAppId(), infrastructureMappingId);
      if (infraMapping instanceof ContainerInfrastructureMapping) {
        containerServiceParams = containerDeploymentManagerHelper.getContainerServiceParams(
            (ContainerInfrastructureMapping) infraMapping, "", context);
      }
    }

    boolean isBindTaskFeatureSet = false;
    if (featureFlagService.isEnabled(FeatureName.BIND_FETCH_FILES_TASK_TO_DELEGATE, app.getAccountId())) {
      isBindTaskFeatureSet = true;
    }

    return GitFetchFilesTaskParams.builder()
        .accountId(app.getAccountId())
        .appId(app.getUuid())
        .isFinalState(isRemoteFetchRequiredForManifest(appManifestMap))
        .gitFetchFilesConfigMap(gitFetchFileConfigMap)
        .containerServiceParams(containerServiceParams)
        .isBindTaskFeatureSet(isBindTaskFeatureSet)
        .build();
  }

  private Map<String, GitFetchFilesConfig> getGitFetchFileConfigMap(
      ExecutionContext context, Application app, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = new HashMap<>();

    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      K8sValuesLocation k8sValuesLocation = entry.getKey();
      ApplicationManifest applicationManifest = entry.getValue();

      if (StoreType.Remote == applicationManifest.getStoreType()
          || StoreType.HelmSourceRepo == applicationManifest.getStoreType()) {
        GitFileConfig gitFileConfig =
            gitFileConfigHelperService.renderGitFileConfig(context, applicationManifest.getGitFileConfig());
        GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
        notNullCheck("Git config not found", gitConfig);
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails(gitConfig, app.getUuid(), null);

        GitFetchFilesConfig gitFetchFileConfig = GitFetchFilesConfig.builder()
                                                     .gitConfig(gitConfig)
                                                     .gitFileConfig(gitFileConfig)
                                                     .encryptedDataDetails(encryptionDetails)
                                                     .build();

        gitFetchFileConfigMap.put(k8sValuesLocation.name(), gitFetchFileConfig);
      }
    }

    return gitFetchFileConfigMap;
  }

  private boolean isRemoteFetchRequiredForManifest(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    if (!appManifestMap.containsKey(K8sValuesLocation.Service)) {
      return true;
    }

    ApplicationManifest applicationManifest = appManifestMap.get(K8sValuesLocation.Service);
    return Local == applicationManifest.getStoreType();
  }

  public boolean isValuesInGit(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Remote == applicationManifest.getStoreType()
          || StoreType.HelmSourceRepo == applicationManifest.getStoreType()) {
        return true;
      }
    }

    return false;
  }

  public Map<K8sValuesLocation, String> getValuesFilesFromGitFetchFilesResponse(
      GitCommandExecutionResponse executionResponse) {
    GitFetchFilesFromMultipleRepoResult gitCommandResult =
        (GitFetchFilesFromMultipleRepoResult) executionResponse.getGitCommandResult();

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    if (gitCommandResult == null || isEmpty(gitCommandResult.getFilesFromMultipleRepo())) {
      return valuesFiles;
    }

    for (Entry<String, GitFetchFilesResult> entry : gitCommandResult.getFilesFromMultipleRepo().entrySet()) {
      GitFetchFilesResult gitFetchFilesResult = entry.getValue();

      if (isNotEmpty(gitFetchFilesResult.getFiles())
          && isNotBlank(gitFetchFilesResult.getFiles().get(0).getFileContent())) {
        valuesFiles.put(
            K8sValuesLocation.valueOf(entry.getKey()), gitFetchFilesResult.getFiles().get(0).getFileContent());
      }
    }

    return valuesFiles;
  }

  public void populateValuesFilesFromAppManifest(
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, Map<K8sValuesLocation, String> valuesFiles) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      K8sValuesLocation k8sValuesLocation = entry.getKey();
      ApplicationManifest applicationManifest = entry.getValue();
      if (Local == applicationManifest.getStoreType()) {
        ManifestFile manifestFile =
            applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
        if (manifestFile != null) {
          valuesFiles.put(k8sValuesLocation, manifestFile.getFileContent());
        }
      }
    }
  }

  public void setValuesPathInGitFetchFilesTaskParams(GitFetchFilesTaskParams gitFetchFilesTaskParams) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = gitFetchFilesTaskParams.getGitFetchFilesConfigMap();

    for (Entry<String, GitFetchFilesConfig> entry : gitFetchFileConfigMap.entrySet()) {
      if (K8sValuesLocation.Service.name().equals(entry.getKey())) {
        GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
        gitFetchFileConfig.getGitFileConfig().setFilePath(
            getValuesYamlGitFilePath(gitFetchFileConfig.getGitFileConfig().getFilePath()));
      }
    }
  }

  public List<String> getHelmValuesYamlFiles(String appId, String templateId) {
    ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, templateId);
    if (serviceTemplate == null) {
      return new ArrayList<>();
    }

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

    ApplicationManifest serviceAppManifest =
        applicationManifestService.getByServiceId(appId, serviceTemplate.getServiceId(), AppManifestKind.VALUES);
    if (serviceAppManifest != null) {
      appManifestMap.put(K8sValuesLocation.ServiceOverride, serviceAppManifest);
    }

    ApplicationManifest appManifest =
        applicationManifestService.getByEnvId(appId, serviceTemplate.getEnvId(), AppManifestKind.VALUES);
    if (appManifest != null) {
      appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, appManifest);
    }

    appManifest = applicationManifestService.getByEnvAndServiceId(
        appId, serviceTemplate.getEnvId(), serviceTemplate.getServiceId(), AppManifestKind.VALUES);
    if (appManifest != null) {
      appManifestMap.put(K8sValuesLocation.Environment, appManifest);
    }

    populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);

    return valuesFiles.values().stream().collect(Collectors.toList());
  }

  public Set<String> listExpressionsFromValuesForService(String appId, String serviceId) {
    Set<String> expressionSet = new HashSet<>();

    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(appId, serviceId, AppManifestKind.K8S_MANIFEST);
    if (applicationManifest == null || Local != applicationManifest.getStoreType()) {
      return expressionSet;
    }

    ManifestFile manifestFile =
        applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
    if (manifestFile == null) {
      return expressionSet;
    }

    Map map = getMapFromValuesFileContent(manifestFile.getFileContent());
    if (map == null) {
      return expressionSet;
    }

    return getValuesExpressionKeysFromMap(map, "", 0);
  }

  public Map<K8sValuesLocation, ApplicationManifest> getApplicationManifests(
      ExecutionContext context, AppManifestKind appManifestKind) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

    ApplicationManifest serviceAppManifest = getApplicationManifestForService(context);
    if (serviceAppManifest != null) {
      appManifestMap.put(K8sValuesLocation.Service, serviceAppManifest);
    }
    appManifestMap.putAll(getOverrideApplicationManifests(context, appManifestKind));
    return appManifestMap;
  }

  public ApplicationManifest getApplicationManifestForService(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();
    Service service = serviceResourceService.get(app.getUuid(), serviceElement.getUuid(), false);

    ApplicationManifest applicationManifest =
        applicationManifestService.getManifestByServiceId(app.getUuid(), serviceElement.getUuid());

    if (service.isK8sV2() && applicationManifest == null) {
      throw new InvalidRequestException("Manifests not found for service.");
    }

    return applicationManifest;
  }

  public boolean isValuesInHelmChartRepo(ExecutionContext context) {
    ApplicationManifest applicationManifest = getApplicationManifestForService(context);
    return applicationManifest != null && StoreType.HelmChartRepo == applicationManifest.getStoreType()
        && applicationManifest.getHelmChartConfig() != null
        && isNotBlank(applicationManifest.getHelmChartConfig().getChartName());
  }
}
