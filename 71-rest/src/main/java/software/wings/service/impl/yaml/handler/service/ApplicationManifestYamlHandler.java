package software.wings.service.impl.yaml.handler.service;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.Yaml;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;

import java.util.List;

@Singleton
@Slf4j
public class ApplicationManifestYamlHandler extends BaseYamlHandler<Yaml, ApplicationManifest> {
  @Inject YamlHelper yamlHelper;
  @Inject ApplicationManifestService applicationManifestService;
  @Inject GitFileConfigHelperService gitFileConfigHelperService;
  @Inject YamlResourceService yamlResourceService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject private HelmChartConfigHelperService helmChartConfigHelperService;

  @Override
  public Yaml toYaml(ApplicationManifest applicationManifest, String appId) {
    return Yaml.builder()
        .type(yamlResourceService.getYamlTypeFromAppManifest(applicationManifest).name())
        .harnessApiVersion(getHarnessApiVersion())
        .storeType(applicationManifest.getStoreType().name())
        .gitFileConfig(getGitFileConfigForToYaml(applicationManifest))
        .helmChartConfig(getHelmChartConfigForToYaml(applicationManifest))
        .kustomizeConfig(applicationManifest.getKustomizeConfig())
        .build();
  }

  @Override
  public ApplicationManifest upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId null for given yaml file:" + yamlFilePath, appId, USER);

    ApplicationManifest previous = get(accountId, yamlFilePath);
    ApplicationManifest applicationManifest = toBean(changeContext);
    applicationManifest.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      applicationManifest.setUuid(previous.getUuid());
      return applicationManifestService.update(applicationManifest);
    } else {
      serviceResourceService.setK8v2ServiceFromAppManifest(
          applicationManifest, applicationManifestService.getAppManifestType(applicationManifest));
      serviceResourceService.setPcfV2ServiceFromAppManifestIfRequired(
          applicationManifest, applicationManifestService.getAppManifestType(applicationManifest));
      return applicationManifestService.create(applicationManifest);
    }
  }

  private ApplicationManifest toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId, USER);

    String envId = null;
    String serviceId = getServiceIdFromYamlPath(appId, filePath);
    if (serviceId == null) {
      envId = getEnvIdFromYamlPath(appId, filePath);
      Service service = yamlHelper.getServiceOverrideFromAppManifestPath(appId, filePath);
      serviceId = (service == null) ? null : service.getUuid();
    }

    StoreType storeType = Enum.valueOf(StoreType.class, yaml.getStoreType());
    AppManifestKind kind = yamlHelper.getAppManifestKindFromPath(filePath);
    GitFileConfig gitFileConfig = getGitFileConfigFromYaml(accountId, appId, yaml, storeType);
    HelmChartConfig helmChartConfig = getHelmChartConfigFromYaml(accountId, appId, yaml, storeType);
    KustomizeConfig kustomizeConfig = getKustomizeConfigFromYaml(yaml, storeType);

    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(serviceId)
                                       .envId(envId)
                                       .storeType(storeType)
                                       .gitFileConfig(gitFileConfig)
                                       .helmChartConfig(helmChartConfig)
                                       .kind(kind)
                                       .kustomizeConfig(kustomizeConfig)
                                       .build();

    manifest.setAppId(appId);
    return manifest;
  }

  private KustomizeConfig getKustomizeConfigFromYaml(Yaml yaml, StoreType storeType) {
    KustomizeConfig kustomizeConfig = yaml.getKustomizeConfig();
    if (kustomizeConfig != null && storeType != StoreType.KustomizeSourceRepo) {
      throw new InvalidRequestException(
          "KustomizeConfig should only be used with KustomizeSourceRepo store type", USER);
    }
    return kustomizeConfig;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ApplicationManifest get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not find Application  for the yaml file: " + yamlFilePath, appId, USER);
    return yamlHelper.getApplicationManifest(appId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    Change change = changeContext.getChange();

    ApplicationManifest applicationManifest = get(change.getAccountId(), change.getFilePath());
    if (applicationManifest == null) {
      return;
    }

    // Dont delete the appManifest if coming from git for service.
    if (isBlank(applicationManifest.getEnvId()) && applicationManifest.getKind() == AppManifestKind.K8S_MANIFEST) {
      logger.info("Deleting the application manifest for service from git is not allowed");
      return;
    }

    applicationManifest.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    applicationManifestService.deleteAppManifest(applicationManifest);
  }

  private GitFileConfig getGitFileConfigForToYaml(ApplicationManifest applicationManifest) {
    if (StoreType.Local == applicationManifest.getStoreType()) {
      return null;
    }

    return gitFileConfigHelperService.getGitFileConfigForToYaml(applicationManifest.getGitFileConfig());
  }

  private GitFileConfig getGitFileConfigFromYaml(String accountId, String appId, Yaml yaml, StoreType storeType) {
    GitFileConfig gitFileConfig = yaml.getGitFileConfig();

    if (gitFileConfig == null) {
      return null;
    }
    if (StoreType.Local == storeType) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Git file config should be null for store type local");
    }

    return gitFileConfigHelperService.getGitFileConfigFromYaml(accountId, appId, gitFileConfig);
  }

  private HelmChartConfig getHelmChartConfigForToYaml(ApplicationManifest applicationManifest) {
    if (StoreType.Local == applicationManifest.getStoreType()) {
      return null;
    }

    return helmChartConfigHelperService.getHelmChartConfigForToYaml(applicationManifest.getHelmChartConfig());
  }

  private HelmChartConfig getHelmChartConfigFromYaml(String accountId, String appId, Yaml yaml, StoreType storeType) {
    HelmChartConfig helmChartConfig = yaml.getHelmChartConfig();
    if (helmChartConfig == null) {
      return null;
    }

    if (StoreType.Local == storeType) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "gitFileConfig cannot be used for Local storeType.");
    }

    return helmChartConfigHelperService.getHelmChartConfigFromYaml(accountId, appId, helmChartConfig);
  }

  private String getServiceIdFromYamlPath(String appId, String filePath) {
    try {
      return yamlHelper.getServiceId(appId, filePath);
    } catch (WingsException ex) {
      return null;
    }
  }

  private String getEnvIdFromYamlPath(String appId, String filePath) {
    try {
      return yamlHelper.getEnvironmentId(appId, filePath);
    } catch (WingsException ex) {
      return null;
    }
  }
}
