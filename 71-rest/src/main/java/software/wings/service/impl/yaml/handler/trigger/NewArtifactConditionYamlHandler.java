package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.Condition;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.trigger.NewArtifactConditionYaml;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class NewArtifactConditionYamlHandler extends ConditionYamlHandler<NewArtifactConditionYaml> {
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;

  @Override
  public NewArtifactConditionYaml toYaml(Condition bean, String appId) {
    ArtifactCondition artifactCondition = (ArtifactCondition) bean;

    return NewArtifactConditionYaml.builder()
        .artifactFilter(artifactCondition.getArtifactFilter())
        .artifactStreamName(artifactCondition.getArtifactStreamName())
        .artifactServerName(artifactCondition.getArtifactServerName())
        .artifactStreamType(artifactCondition.getArtifactStreamType())
        .build();
  }

  @Override
  public Condition upsertFromYaml(
      ChangeContext<NewArtifactConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    NewArtifactConditionYaml yaml = changeContext.getYaml();
    String artifactServerId = getArtifactServerIdByName(yaml.getArtifactServerName(), appId, accountId);

    ArtifactStream artifactStream =
        artifactStreamService.getArtifactStreamByName(artifactServerId, yaml.getArtifactStreamName());

    notNullCheck("Could not locate artifact stream info in file path:" + change.getFilePath(), artifactStream, USER);

    return ArtifactCondition.builder()
        .artifactStreamName(yaml.getArtifactStreamName())
        .artifactServerName(yaml.getArtifactServerName())
        .artifactStreamType(yaml.getArtifactStreamType())
        .artifactFilter(yaml.getArtifactFilter())
        .artifactStreamId(artifactStream.getUuid())
        .artifactServerId(artifactServerId)
        .build();
  }

  private String getArtifactServerIdByName(String settingName, String appId, String accountId) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, settingName);
    notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  @Override
  public Class getYamlClass() {
    return NewArtifactConditionYaml.class;
  }
}
