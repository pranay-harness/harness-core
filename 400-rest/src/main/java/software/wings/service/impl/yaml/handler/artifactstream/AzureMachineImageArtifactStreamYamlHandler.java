/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AzureMachineImageArtifactStream;
import software.wings.beans.artifact.AzureMachineImageArtifactStream.Yml;
import software.wings.beans.yaml.ChangeContext;

@OwnedBy(CDC)
public class AzureMachineImageArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<Yml, AzureMachineImageArtifactStream> {
  @Override
  protected AzureMachineImageArtifactStream getNewArtifactStreamObject() {
    return new AzureMachineImageArtifactStream();
  }

  @Override
  public Yml toYaml(AzureMachineImageArtifactStream bean, String appId) {
    Yml yml = Yml.builder().build();
    super.toYaml(yml, bean);
    yml.setSubscriptionId(bean.getSubscriptionId());
    yml.setImageType(bean.getImageType());
    yml.setImageDefinition(bean.getImageDefinition());
    return yml;
  }

  @Override
  public Class getYamlClass() {
    return AzureMachineImageArtifactStream.Yml.class;
  }

  @Override
  protected void toBean(AzureMachineImageArtifactStream bean, ChangeContext<Yml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yml yml = changeContext.getYaml();
    bean.setImageType(yml.getImageType());
    bean.setImageDefinition(yml.getImageDefinition());
    bean.setSubscriptionId(yml.getSubscriptionId());
  }
}
