package software.wings.service.impl.yaml.handler.service;

import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.Yaml;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.yaml.YamlResourceService;

import java.util.List;

@Singleton
public class ApplicationManifestYamlHandler extends BaseYamlHandler<Yaml, ApplicationManifest> {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationManifestYamlHandler.class);

  @Inject YamlHelper yamlHelper;
  @Inject ApplicationManifestService applicationManifestService;
  @Inject GitFileConfigHelperService gitFileConfigHelperService;
  @Inject YamlResourceService yamlResourceService;

  @Override
  public Yaml toYaml(ApplicationManifest applicationManifest, String appId) {
    return Yaml.builder()
        .type(yamlResourceService.getYamlTypeFromAppManifest(applicationManifest).name())
        .harnessApiVersion(getHarnessApiVersion())
        .storeType(applicationManifest.getStoreType().name())
        .gitFileConfig(getGitFileConfigForToYaml(applicationManifest))
        .build();
  }

  @Override
  public ApplicationManifest upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
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
    GitFileConfig gitFileConfig = getGitFileConfigFromYaml(accountId, appId, yaml, storeType);

    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(serviceId)
                                       .envId(envId)
                                       .storeType(storeType)
                                       .gitFileConfig(gitFileConfig)
                                       .build();

    manifest.setAppId(appId);
    return manifest;
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
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Change change = changeContext.getChange();

    ApplicationManifest applicationManifest = get(change.getAccountId(), change.getFilePath());
    if (applicationManifest == null) {
      return;
    }

    // Dont delete the appManifest if coming from git for service.
    if (isBlank(applicationManifest.getEnvId())) {
      throw new UnsupportedOperationException("Deleting the application manifest for service from git is not allowed");
    }

    applicationManifest.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    applicationManifestService.deleteAppManifest(applicationManifest);
  }

  private GitFileConfig getGitFileConfigForToYaml(ApplicationManifest applicationManifest) {
    if (StoreType.Local.equals(applicationManifest.getStoreType())) {
      return null;
    }

    return gitFileConfigHelperService.getGitFileConfigForToYaml(applicationManifest.getGitFileConfig());
  }

  private GitFileConfig getGitFileConfigFromYaml(String accountId, String appId, Yaml yaml, StoreType storeType) {
    GitFileConfig gitFileConfig = yaml.getGitFileConfig();

    if (gitFileConfig == null) {
      return null;
    }
    if (StoreType.Local.equals(storeType)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Git file config should be null for store type local");
    }

    return gitFileConfigHelperService.getGitFileConfigFromYaml(accountId, appId, gitFileConfig);
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
