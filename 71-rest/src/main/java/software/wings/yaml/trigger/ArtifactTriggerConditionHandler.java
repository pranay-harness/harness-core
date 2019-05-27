package software.wings.yaml.trigger;

import com.google.inject.Singleton;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class ArtifactTriggerConditionHandler extends TriggerConditionYamlHandler<ArtifactTriggerConditionYaml> {
  @Override
  public ArtifactTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) bean;

    String artifactStreamId = artifactTriggerCondition.getArtifactStreamId();

    return ArtifactTriggerConditionYaml.builder()
        .serviceName(yamlHelper.getServiceNameFromArtifactId(appId, artifactStreamId))
        .artifactFilter(artifactTriggerCondition.getArtifactFilter())
        .artifactStreamName(yamlHelper.getArtifactStreamName(appId, artifactStreamId))
        .regex(artifactTriggerCondition.isRegex())
        .build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<ArtifactTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    ArtifactTriggerConditionYaml artifactTriggerConditionYaml = (ArtifactTriggerConditionYaml) yaml;
    String serviceName = artifactTriggerConditionYaml.getServiceName();
    String artifactStreamName = artifactTriggerConditionYaml.getArtifactStreamName();
    ArtifactStream artifactStream = yamlHelper.getArtifactStreamWithName(appId, serviceName, artifactStreamName);

    return ArtifactTriggerCondition.builder()
        .artifactFilter(artifactTriggerConditionYaml.getArtifactFilter())
        .artifactStreamId(artifactStream.getUuid())
        .artifactSourceName(artifactStream.generateSourceName())
        .regex(artifactTriggerConditionYaml.isRegex())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return ArtifactTriggerConditionYaml.class;
  }
}
