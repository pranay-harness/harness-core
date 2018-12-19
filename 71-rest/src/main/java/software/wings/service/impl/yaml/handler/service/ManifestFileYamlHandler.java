package software.wings.service.impl.yaml.handler.service;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.ManifestFile.Yaml;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ManifestFileYamlHandler extends BaseYamlHandler<Yaml, ManifestFile> {
  private static final Logger logger = LoggerFactory.getLogger(ManifestFileYamlHandler.class);
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ApplicationManifestService applicationManifestService;

  @Override
  public Yaml toYaml(ManifestFile manifestFile, String appId) {
    return Yaml.builder().harnessApiVersion(getHarnessApiVersion()).fileContent(manifestFile.getFileContent()).build();
  }

  @Override
  public ManifestFile upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId null for given yaml file:" + yamlFilePath, appId, USER);

    String serviceName = yamlHelper.getServiceName(yamlFilePath);
    Service service = serviceResourceService.getServiceByName(appId, serviceName);
    notNullCheck("Service null for given yaml file:" + yamlFilePath, service, USER);

    String fileName = getActualFileName(yamlFilePath);
    ManifestFile previous = yamlHelper.getManifestFile(appId, yamlFilePath, fileName);

    ManifestFile manifestFile = toBean(changeContext, fileName);
    manifestFile.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    boolean isCreate = true;
    if (previous != null) {
      manifestFile.setUuid(previous.getUuid());
      isCreate = false;
    }
    return applicationManifestService.upsertApplicationManifestFile(manifestFile, service.getUuid(), isCreate);
  }

  /**
   * Remove path prefix, Setup/Application/....Manifests/Files/ and return actual file name
   * @param yamlFilePath
   * @return
   */
  private String getActualFileName(String yamlFilePath) {
    Pattern pattern = Pattern.compile(YamlType.MANIFEST_FILE.getPrefixExpression());
    Matcher matcher = pattern.matcher(yamlFilePath);
    String prefix = matcher.find() ? matcher.group(0) : null;

    if (prefix == null) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message",
              "Invalid YamlFile Path for Manifest File, required Prefix not present in Path: " + yamlFilePath);
    }

    return yamlFilePath.substring(prefix.length());
  }

  private ManifestFile toBean(ChangeContext<Yaml> changeContext, String fileName) throws HarnessException {
    Change change = changeContext.getChange();

    String yamlFilePath = changeContext.getChange().getFilePath();

    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId, USER);

    ApplicationManifest applicationManifest = applicationManifestService.getByServiceId(appId, serviceId);
    notNullCheck("ApplicationManifest null for given yaml file:" + yamlFilePath, applicationManifest, USER);

    ManifestFile manifestFile = ManifestFile.builder()
                                    .fileName(fileName)
                                    .fileContent(change.getFileContent())
                                    .applicationManifestId(applicationManifest.getUuid())
                                    .build();
    manifestFile.setAppId(appId);
    return manifestFile;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ManifestFile get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not find Application  for the yaml file: " + yamlFilePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not find Service for the yaml file: " + yamlFilePath, serviceId, USER);

    ApplicationManifest applicationManifest = applicationManifestService.getByServiceId(appId, serviceId);
    notNullCheck("ApplicationManifest null for given yaml file:" + yamlFilePath, applicationManifest, USER);

    return applicationManifestService.getManifestFileByFileName(
        applicationManifest.getUuid(), getActualFileName(yamlFilePath));
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Change change = changeContext.getChange();
    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());

    ManifestFile manifestFile = get(change.getAccountId(), change.getFilePath());
    if (manifestFile != null) {
      applicationManifestService.deleteManifestFileById(appId, manifestFile.getUuid());
    }
  }
}
