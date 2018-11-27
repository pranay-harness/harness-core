package software.wings.service.impl.yaml.handler.service;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.Yaml;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;

import java.util.List;

@Singleton
public class ApplicationManifestYamlHandler extends BaseYamlHandler<Yaml, ApplicationManifest> {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationManifestYamlHandler.class);
  @Inject YamlHelper yamlHelper;
  @Inject ApplicationManifestService applicationManifestService;

  @Override
  public Yaml toYaml(ApplicationManifest applicationManifest, String appId) {
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .storeType(applicationManifest.getStoreType().name())
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
      applicationManifestService.update(applicationManifest);
    } else {
      applicationManifestService.create(applicationManifest);
    }

    return null;
  }

  private ApplicationManifest toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId, USER);

    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(serviceId)
                                       .storeType(Enum.valueOf(StoreType.class, yaml.getStoreType()))
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
    throw new UnsupportedOperationException();
  }
}
