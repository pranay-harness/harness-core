/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import com.google.inject.Singleton;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
@TargetModule(HarnessModule._815_CG_TRIGGERS)
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
    ArtifactStream artifactStream = null;
    String artiFactStreamId = null;
    String artifactSourceName = null;
    if (EmptyPredicate.isNotEmpty(serviceName) && EmptyPredicate.isNotEmpty(artifactStreamName)) {
      artifactStream = yamlHelper.getArtifactStreamWithName(appId, serviceName, artifactStreamName);
      notNullCheck(format("Artifact stream [%s] does not exist", artifactStreamName), artifactStream, USER);
      artiFactStreamId = artifactStream.getUuid();
      artifactSourceName = artifactStream.generateSourceName();
    }

    return ArtifactTriggerCondition.builder()
        .artifactFilter(artifactTriggerConditionYaml.getArtifactFilter())
        .artifactStreamId(artiFactStreamId)
        .artifactSourceName(artifactSourceName)
        .regex(artifactTriggerConditionYaml.isRegex())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return ArtifactTriggerConditionYaml.class;
  }
}
