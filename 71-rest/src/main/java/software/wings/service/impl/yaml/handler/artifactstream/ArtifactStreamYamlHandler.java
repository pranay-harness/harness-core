package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.template.TemplateHelper.obtainTemplateVersion;

import com.google.inject.Inject;

import io.harness.exception.HarnessException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.Yaml;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;
import java.util.Optional;

/**
 * @author rktummala on 10/09/17
 */
@Slf4j
public abstract class ArtifactStreamYamlHandler<Y extends Yaml, B extends ArtifactStream>
    extends BaseYamlHandler<Y, B> {
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject YamlHelper yamlHelper;
  @Inject private TemplateService templateService;
  @Inject private FeatureFlagService featureFlagService;

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
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
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
        artifactStreamService.deleteWithBinding(
            application.getUuid(), artifactStream.getUuid(), false, changeContext.getChange().isSyncFromGit());
      }
    } else {
      if (changeContext.getYamlType().equals(YamlType.ARTIFACT_STREAM)) {
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
          artifactStreamService.deleteWithBinding(
              application.getUuid(), artifactStream.getUuid(), false, changeContext.getChange().isSyncFromGit());
        }
      } else {
        ArtifactStream artifactStream = yamlHelper.getArtifactStream(accountId, yamlFilePath);
        if (artifactStream != null) {
          artifactStreamService.deleteWithBinding(
              GLOBAL_APP_ID, artifactStream.getUuid(), false, changeContext.getChange().isSyncFromGit());
        }
      }
    }
  }

  protected void toYaml(Y yaml, B bean) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, bean.getAccountId())) {
      yaml.setServerName(getSettingName(bean.getSettingId()));
    } else {
      if (!CUSTOM.name().equals(yaml.getType())) {
        yaml.setServerName(getSettingName(bean.getSettingId()));
      }
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
      toBean(previous, changeContext, previous.fetchAppId());
      previous.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      return (B) artifactStreamService.update(previous, !previous.isSyncFromGit());
    } else {
      if (!featureFlagService.isEnabled(
              FeatureName.ARTIFACT_STREAM_REFACTOR, changeContext.getChange().getAccountId())) {
        String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
        String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);

        B artifactStream = getNewArtifactStreamObject();
        artifactStream.setServiceId(serviceId);
        artifactStream.setAppId(appId);
        toBean(artifactStream, changeContext, appId);
        artifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());
        return (B) artifactStreamService.createWithBinding(appId, artifactStream, !artifactStream.isSyncFromGit());
      } else {
        if (changeContext.getYamlType().equals(YamlType.ARTIFACT_STREAM)) {
          String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
          String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);

          B artifactStream = getNewArtifactStreamObject();
          artifactStream.setServiceId(serviceId);
          artifactStream.setAppId(appId);
          toBean(artifactStream, changeContext, appId);
          artifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());
          return (B) artifactStreamService.createWithBinding(appId, artifactStream, !artifactStream.isSyncFromGit());
        } else {
          B artifactStream = getNewArtifactStreamObject();
          artifactStream.setAppId(GLOBAL_APP_ID);
          toBean(artifactStream, changeContext, GLOBAL_APP_ID);
          artifactStream.setSyncFromGit(changeContext.getChange().isSyncFromGit());
          return (B) artifactStreamService.createWithBinding(
              GLOBAL_APP_ID, artifactStream, !artifactStream.isSyncFromGit());
        }
      }
    }
  }

  protected void toBean(B bean, ChangeContext<Y> changeContext, String appId) {
    Y yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setName(name);
    bean.setAutoPopulate(false);
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      bean.setSettingId(getSettingId(changeContext.getChange().getAccountId(), appId, yaml.getServerName()));
    } else {
      if (!CUSTOM.name().equals(yaml.getType())) {
        bean.setSettingId(getSettingId(changeContext.getChange().getAccountId(), appId, yaml.getServerName()));
      }
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
