package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public abstract class ArtifactStreamYamlHandler<Y extends Yaml, B extends ArtifactStream>
    extends BaseYamlHandler<Y, B> {
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject YamlHelper yamlHelper;

  protected String getSettingId(String accountId, String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, settingName);
    Validator.notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute);
    return settingAttribute.getUuid();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    Validator.notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute);
    return settingAttribute.getName();
  }

  protected B getArtifactStream(String accountId, String yamlFilePath) {
    Validator.notNullCheck("Yaml file path is null", yamlFilePath);
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
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Application can't be found for yaml file:" + yamlFilePath, appId);
    ArtifactStream artifactStream = yamlHelper.getArtifactStream(accountId, yamlFilePath);
    if (artifactStream != null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
    }
  }

  protected void toYaml(Y yaml, B bean) {
    yaml.setServerName(getSettingName(bean.getSettingId()));
    yaml.setMetadataOnly(bean.isMetadataOnly());
    yaml.setHarnessApiVersion(getHarnessApiVersion());
  }

  @Override
  public B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }
    String yamlFilePath = changeContext.getChange().getFilePath();
    B previous = get(changeContext.getChange().getAccountId(), yamlFilePath);
    if (previous != null) {
      toBean(previous, changeContext, previous.getAppId());
      return (B) artifactStreamService.update(previous);

    } else {
      String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
      String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);

      B artifactStream = getNewArtifactStreamObject();
      artifactStream.setServiceId(serviceId);
      artifactStream.setAppId(appId);
      toBean(artifactStream, changeContext, appId);
      return (B) artifactStreamService.create(artifactStream);
    }
  }

  protected void toBean(B bean, ChangeContext<Y> changeContext, String appId) {
    Y yaml = changeContext.getYaml();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setName(name);
    bean.setAutoPopulate(false);
    bean.setSettingId(getSettingId(changeContext.getChange().getAccountId(), appId, yaml.getServerName()));
    bean.setAutoApproveForProduction(true);
    bean.setMetadataOnly(yaml.isMetadataOnly());
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getServerName()));
  }

  protected abstract B getNewArtifactStreamObject();
}
