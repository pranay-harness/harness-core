package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.trigger.TriggerArtifactSelectionLastCollectedYaml;

import java.util.List;

@OwnedBy(CDC)
public class TriggerArtifactLastCollectedYamlHandler
    extends TriggerArtifactValueYamlHandler<TriggerArtifactSelectionLastCollectedYaml> {
  @Inject ArtifactStreamService artifactStreamService;
  @Inject SettingsService settingsService;
  @Inject YamlHelper yamlHelper;

  @Override
  public TriggerArtifactSelectionLastCollectedYaml toYaml(TriggerArtifactSelectionValue bean, String appId) {
    TriggerArtifactSelectionLastCollected triggerArtifactSelectionLastCollected =
        (TriggerArtifactSelectionLastCollected) bean;

    ArtifactStream artifactStream =
        artifactStreamService.get(triggerArtifactSelectionLastCollected.getArtifactStreamId());

    return TriggerArtifactSelectionLastCollectedYaml.builder()
        .artifactFilter(triggerArtifactSelectionLastCollected.getArtifactFilter())
        .artifactStreamName(artifactStream.getName())
        .artifactStreamType(artifactStream.getArtifactStreamType())
        .artifactServerName(artifactStream.getArtifactServerName())
        .build();
  }

  @Override
  public TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<TriggerArtifactSelectionLastCollectedYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();

    String appId = yamlHelper.getAppId(accountId, change.getFilePath());

    TriggerArtifactSelectionLastCollectedYaml yaml = changeContext.getYaml();
    String artifactServerId = getArtifactServerIdByName(yaml.getArtifactServerName(), appId, accountId);
    ArtifactStream artifactStream =
        artifactStreamService.getArtifactStreamByName(artifactServerId, yaml.getArtifactStreamName());
    notNullCheck("Invalid artifact stream", artifactStream, USER);

    return TriggerArtifactSelectionLastCollected.builder()
        .artifactFilter(yaml.getArtifactFilter())
        .artifactServerName(yaml.getArtifactServerName())
        .artifactStreamName(yaml.getArtifactStreamName())
        .artifactStreamType(yaml.getArtifactStreamType())
        .artifactServerId(artifactServerId)
        .artifactStreamId(artifactStream.getUuid())
        .build();
  }

  private String getArtifactServerIdByName(String settingName, String appId, String accountId) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, settingName);
    notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  @Override
  public Class getYamlClass() {
    return TriggerArtifactSelectionLastCollectedYaml.class;
  }
}
