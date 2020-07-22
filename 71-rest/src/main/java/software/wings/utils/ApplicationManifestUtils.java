package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.manifest.ManifestHelper.getMapFromValuesFileContent;
import static io.harness.k8s.manifest.ManifestHelper.getValuesExpressionKeysFromMap;
import static io.harness.k8s.manifest.ManifestHelper.getValuesYamlGitFilePath;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.StringUtils;
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
import software.wings.beans.yaml.GitFile;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ApplicationManifestUtils {
  private static final String MULTIPLE_FILES_DELIMITER = ",";

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

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);

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
    String infrastructureMappingId = context == null ? null : context.fetchInfraMappingId();

    if (infrastructureMappingId != null) {
      InfrastructureMapping infraMapping =
          infrastructureMappingService.get(context.getAppId(), infrastructureMappingId);
      if (infraMapping instanceof ContainerInfrastructureMapping) {
        containerServiceParams = containerDeploymentManagerHelper.getContainerServiceParams(
            (ContainerInfrastructureMapping) infraMapping, "", context);
      }
    }

    boolean isBindTaskFeatureSet =
        featureFlagService.isEnabled(FeatureName.BIND_FETCH_FILES_TASK_TO_DELEGATE, app.getAccountId());

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
        // use env override if available. We do not support merge config at service and env for HelmChartConfig
        Service service = fetchServiceFromContext(context);
        if (service.isK8sV2() && StoreType.HelmSourceRepo == applicationManifest.getStoreType()) {
          applicationManifest = getAppManifestByApplyingHelmChartOverride(context);
        }

        GitFileConfig gitFileConfig =
            gitFileConfigHelperService.renderGitFileConfig(context, applicationManifest.getGitFileConfig());
        GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
        notNullCheck("Git config not found", gitConfig);
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails(gitConfig, app.getUuid(), context.getWorkflowExecutionId());

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

  public ApplicationManifest getAppManifestByApplyingHelmChartOverride(ExecutionContext context) {
    ApplicationManifest manifestAtService = getApplicationManifestForService(context);
    if (manifestAtService == null
        || (HelmChartRepo != manifestAtService.getStoreType() && HelmSourceRepo != manifestAtService.getStoreType())) {
      return null;
    }

    Map<K8sValuesLocation, ApplicationManifest> manifestsMap =
        getOverrideApplicationManifests(context, HELM_CHART_OVERRIDE);

    // Priority: service override in env > global override in env > service
    // chart name chart repo should not come from overrides of any kind
    applyHelmChartOverride(manifestAtService, manifestsMap);

    return manifestAtService;
  }

  public void applyHelmChartOverride(
      ApplicationManifest manifestAtService, Map<K8sValuesLocation, ApplicationManifest> manifestsMap) {
    final K8sValuesLocation overrideEnvironmentSelected = manifestsMap.containsKey(K8sValuesLocation.Environment)
        ? K8sValuesLocation.Environment
        : K8sValuesLocation.EnvironmentGlobal;
    if (HelmChartRepo == manifestAtService.getStoreType()) {
      if (overrideEnvironmentSelected == K8sValuesLocation.Environment
          || overrideEnvironmentSelected == K8sValuesLocation.EnvironmentGlobal) {
        applyK8sValuesLocationBasedHelmChartOverride(manifestAtService, manifestsMap, overrideEnvironmentSelected);
      }
    }
  }

  public void applyK8sValuesLocationBasedHelmChartOverride(ApplicationManifest manifestAtService,
      Map<K8sValuesLocation, ApplicationManifest> manifestsMap, K8sValuesLocation k8sValuesLocation) {
    ApplicationManifest applicationManifestAtK8sLocation = manifestsMap.get(k8sValuesLocation);
    if (applicationManifestAtK8sLocation != null) {
      throwExceptionIfStoreTypesDontMatch(applicationManifestAtK8sLocation, manifestAtService);
      // only override helm connector
      manifestAtService.getHelmChartConfig().setConnectorId(
          applicationManifestAtK8sLocation.getHelmChartConfig().getConnectorId());
    }
  }

  private void throwExceptionIfStoreTypesDontMatch(
      ApplicationManifest manifest, ApplicationManifest manifestAtService) {
    if (manifest.getStoreType() != manifestAtService.getStoreType()) {
      throw new InvalidRequestException(new StringBuilder("Environment Override should not change Manifest Format. ")
                                            .append(getManifestFormatName(manifestAtService.getStoreType()))
                                            .append(" is mentioned at Service, but mentioned as ")
                                            .append(getManifestFormatName(manifest.getStoreType()))
                                            .append(" at Environment Global Override")
                                            .toString());
    }
  }

  private String getManifestFormatName(StoreType storeType) {
    StringBuilder stringBuilder = new StringBuilder(128).append('"');
    if (HelmChartRepo == storeType) {
      stringBuilder.append("Helm Chart from Helm Repository");
    } else {
      stringBuilder.append("Helm Chart from Source Repository");
    }

    stringBuilder.append('"');
    return stringBuilder.toString();
  }

  public Map<K8sValuesLocation, Collection<String>> getValuesFilesFromGitFetchFilesResponse(
      Map<K8sValuesLocation, ApplicationManifest> appManifest, GitCommandExecutionResponse response) {
    GitFetchFilesFromMultipleRepoResult gitCommandResult =
        (GitFetchFilesFromMultipleRepoResult) response.getGitCommandResult();

    Multimap<K8sValuesLocation, String> valuesFiles = ArrayListMultimap.create();
    if (gitCommandResult == null || isEmpty(gitCommandResult.getFilesFromMultipleRepo())) {
      return valuesFiles.asMap();
    }

    for (Entry<String, GitFetchFilesResult> entry : gitCommandResult.getFilesFromMultipleRepo().entrySet()) {
      GitFetchFilesResult gitFetchFilesResult = entry.getValue();
      Map<String, GitFile> namedGitFiles = new LinkedHashMap<>();
      K8sValuesLocation k8sValuesLocation = K8sValuesLocation.valueOf(entry.getKey());
      for (GitFile file : gitFetchFilesResult.getFiles()) {
        if (isNotBlank(file.getFileContent())) {
          namedGitFiles.put(file.getFilePath(), file);
        }
      }

      ApplicationManifest manifest = appManifest.get(k8sValuesLocation);
      valuesFiles.putAll(k8sValuesLocation, getGitOrderedFiles(k8sValuesLocation, manifest, namedGitFiles));
    }

    return valuesFiles.asMap();
  }

  private Collection<String> getGitOrderedFiles(
      K8sValuesLocation location, ApplicationManifest appManifest, Map<String, GitFile> gitFiles) {
    // Will always expect to get only single file from application Service manifest or none if doesn't exists
    if (K8sValuesLocation.Service == location) {
      return isNotEmpty(gitFiles) ? singletonList(gitFiles.values().iterator().next().getFileContent()) : emptyList();
    }

    return appManifest.getGitFileConfig()
        .getFilePathList()
        .stream()
        .map(filePath -> getFileOrThrowException(filePath, gitFiles, location))
        .map(GitFile::getFileContent)
        .collect(Collectors.toList());
  }

  private GitFile getFileOrThrowException(String filePath, Map<String, GitFile> gitFiles, K8sValuesLocation location) {
    if (!gitFiles.containsKey(filePath)) {
      String message = new StringBuilder()
                           .append("Unable to match any files using path '")
                           .append(filePath)
                           .append("' for ")
                           .append(location)
                           .append(" Values YAML. ")
                           .append("Please ensure that '")
                           .append(filePath)
                           .append("' is an actual path to an existing file and not a directory")
                           .toString();
      throw new InvalidRequestException(message);
    }

    return gitFiles.get(filePath);
  }

  public void populateValuesFilesFromAppManifest(Map<K8sValuesLocation, ApplicationManifest> appManifestMap,
      Map<K8sValuesLocation, Collection<String>> multipleValuesFiles) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      K8sValuesLocation k8sValuesLocation = entry.getKey();
      ApplicationManifest applicationManifest = entry.getValue();
      getManifestFileContentIfExists(applicationManifest)
          .ifPresent(fileContent -> multipleValuesFiles.put(k8sValuesLocation, singletonList(fileContent)));
    }
  }

  private Optional<String> getManifestFileContentIfExists(ApplicationManifest manifest) {
    if (Local == manifest.getStoreType()) {
      ManifestFile manifestFile = applicationManifestService.getManifestFileByFileName(
          manifest.getUuid(), manifest.getKind().getDefaultFileName());
      if (manifestFile != null && isNotBlank(manifestFile.getFileContent())) {
        return Optional.of(manifestFile.getFileContent());
      }
    }

    return Optional.empty();
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

    Map<K8sValuesLocation, Collection<String>> valuesFiles = new EnumMap<>(K8sValuesLocation.class);
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);

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

    return valuesFiles.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
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
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);

    ApplicationManifest serviceAppManifest = getApplicationManifestForService(context);
    if (serviceAppManifest != null) {
      appManifestMap.put(K8sValuesLocation.Service, serviceAppManifest);
    }
    appManifestMap.putAll(getOverrideApplicationManifests(context, appManifestKind));
    return appManifestMap;
  }

  public ApplicationManifest getApplicationManifestForService(ExecutionContext context) {
    Service service = fetchServiceFromContext(context);
    ApplicationManifest applicationManifest =
        applicationManifestService.getManifestByServiceId(context.getAppId(), service.getUuid());

    if (service.isK8sV2() && applicationManifest == null) {
      throw new InvalidRequestException("Manifests not found for service.");
    }

    return applicationManifest;
  }

  public Service fetchServiceFromContext(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();
    return serviceResourceService.get(app.getUuid(), serviceElement.getUuid(), false);
  }

  public boolean isValuesInHelmChartRepo(ExecutionContext context) {
    ApplicationManifest applicationManifest = getApplicationManifestForService(context);
    return applicationManifest != null && StoreType.HelmChartRepo == applicationManifest.getStoreType()
        && applicationManifest.getHelmChartConfig() != null
        && isNotBlank(applicationManifest.getHelmChartConfig().getChartName());
  }

  public boolean isKustomizeSource(ExecutionContext context) {
    ApplicationManifest appManifest = getApplicationManifestForService(context);
    return appManifest != null && appManifest.getStoreType() == KustomizeSourceRepo;
  }

  public void populateRemoteGitConfigFilePathList(
      ExecutionContext context, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    appManifestMap.entrySet()
        .stream()
        // Should support multiple files only for Values YAML overrides and not Service manifest values.yaml file
        .filter(entry -> K8sValuesLocation.Service != entry.getKey())
        .filter(entry -> {
          if (isEmpty(entry.getValue().getGitFileConfig().getFilePath())) {
            throw new InvalidRequestException("Empty file path is not allowed for " + entry.getKey() + " Values YAML");
          }

          return true;
        })
        .map(Entry::getValue)
        .map(ApplicationManifest::getGitFileConfig)
        .filter(Objects::nonNull)
        .forEach(gitFileConfig -> splitGitFileConfigFilePath(context, gitFileConfig));
  }

  private void splitGitFileConfigFilePath(ExecutionContext context, GitFileConfig gitFileConfig) {
    String filePath = gitFileConfig.getFilePath();
    String renderedFilePath = context.renderExpression(normalizeMultipleFilesFilePath(filePath));
    List<String> multipleFiles = Arrays.stream(renderedFilePath.split(MULTIPLE_FILES_DELIMITER))
                                     .map(String::trim)
                                     .filter(value -> validateFilePath(value, filePath))
                                     .collect(Collectors.toList());
    gitFileConfig.setFilePath(null);
    gitFileConfig.setFilePathList(multipleFiles);
  }

  private String normalizeMultipleFilesFilePath(String filePath) {
    // Transform from filePath <,file1,file2,file3,> to <file1,file2,file3>
    return Arrays.stream(filePath.split(MULTIPLE_FILES_DELIMITER))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.joining(MULTIPLE_FILES_DELIMITER));
  }

  private boolean validateFilePath(String value, String originalValue) {
    // expressions like <${valid}, ${missingValue}> could lead to result like <value, null>
    if (isEmpty(value) || value.equals("null")) {
      throw new InvalidRequestException(
          "Invalid file path '" + value + "' after resolving value '" + originalValue + "'");
    }

    return true;
  }

  public void renderGitConfigForApplicationManifest(
      ExecutionContext context, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    appManifestMap.forEach((location, manifest) -> {
      if (manifest.getGitFileConfig() != null) {
        GitFileConfig gitFileConfig = manifest.getGitFileConfig();
        gitFileConfigHelperService.renderGitFileConfig(context, gitFileConfig);
      }
    });
  }
}
