package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.template.TemplateHelper.obtainTemplateVersion;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.exception.HarnessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.Yaml;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;
import java.util.Optional;

/**
 * @author rktummala on 10/09/17
 */
public abstract class ArtifactStreamYamlHandler<Y extends Yaml, B extends ArtifactStream>
    extends BaseYamlHandler<Y, B> {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactStreamYamlHandler.class);
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject YamlHelper yamlHelper;
  @Inject private TemplateService templateService;

  protected String getSettingId(String accountId, String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, settingName);
    notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute, USER);
    return settingAttribute.getName();
  }

  protected B getArtifactStream(String accountId, String yamlFilePath) {
    notNullCheck("Yaml file path is null", yamlFilePath, USER);
    return (B) yamlHelper.getArtifactStream(accountId, yamlFilePath);
  }

  @Override
  public B get(String accountId, String yamlFilePath) {
    return getArtifactStream(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Application application = optionalApplication.get();
    Optional<Service> serviceOptional = yamlHelper.getServiceIfPresent(application.getUuid(), yamlFilePath);
    if (!serviceOptional.isPresent()) {
      return;
    }

    ArtifactStream artifactStream =
        yamlHelper.getArtifactStream(application.getUuid(), serviceOptional.get().getUuid(), yamlFilePath);
    if (artifactStream != null) {
      artifactStreamService.deleteByYamlGit(
          application.getUuid(), artifactStream.getUuid(), changeContext.getChange().isSyncFromGit());
    }
  }

  protected void toYaml(Y yaml, B bean) {
    if (!CUSTOM.name().equals(yaml.getType())) {
      yaml.setServerName(getSettingName(bean.getSettingId()));
      yaml.setMetadataOnly(bean.isMetadataOnly());
    }
    yaml.setHarnessApiVersion(getHarnessApiVersion());
    String templateUri = null;
    String templateUuid = bean.getTemplateUuid();
    if (templateUuid != null) {
      // ArtifactStream is linked
      templateUri = templateService.fetchTemplateUri(templateUuid);
      if (templateUri == null) {
        logger.warn("Linked template for Artifact Source template  {} was deleted", templateUuid);
      }
      if (bean.getTemplateVersion() != null) {
        templateUri = templateUri + ":" + bean.getTemplateVersion();
      }
    }
    yaml.setTemplateUri(templateUri);
    yaml.setTemplateVariables(TemplateHelper.convertToTemplateVariables(bean.getTemplateVariables()));
  }

  @Override
  public B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    B previous = get(changeContext.getChange().getAccountId(), yamlFilePath);
    if (previous != null) {
      toBean(previous, changeContext, previous.getAppId());
      previous.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      return (B) artifactStreamService.update(previous, !previous.isSyncFromGit());

    } else {
      String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
      String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);

      B artifactStream = getNewArtifactStreamObject();
      artifactStream.setServiceId(serviceId);
      artifactStream.setAppId(appId);
      toBean(artifactStream, changeContext, appId);
      artifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      return (B) artifactStreamService.create(artifactStream, !artifactStream.isSyncFromGit());
    }
  }

  protected void toBean(B bean, ChangeContext<Y> changeContext, String appId) {
    Y yaml = changeContext.getYaml();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setName(name);
    bean.setAutoPopulate(false);
    if (!CUSTOM.name().equals(yaml.getType())) {
      bean.setSettingId(getSettingId(changeContext.getChange().getAccountId(), appId, yaml.getServerName()));
      bean.setMetadataOnly(yaml.isMetadataOnly());
    }

    String templateUri = yaml.getTemplateUri();
    if (isNotEmpty(templateUri)) {
      bean.setTemplateUuid(
          templateService.fetchTemplateIdFromUri(changeContext.getChange().getAccountId(), templateUri));
      bean.setTemplateVersion(obtainTemplateVersion(templateUri));
    }
    bean.setTemplateVariables(TemplateHelper.convertToEntityVariables(yaml.getTemplateVariables()));
  }

  protected abstract B getNewArtifactStreamObject();
}
