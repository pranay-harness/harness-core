/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class AzureArtifactsArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<Yaml, AzureArtifactsArtifactStream> {
  @Override
  public Yaml toYaml(AzureArtifactsArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setPackageType(bean.getProtocolType());
    yaml.setProject(bean.getProject());
    yaml.setFeed(bean.getFeed());
    yaml.setPackageId(bean.getPackageId());
    yaml.setPackageName(bean.getPackageName());
    return yaml;
  }

  @Override
  protected void toBean(AzureArtifactsArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setProtocolType(yaml.getPackageType());
    bean.setProject(yaml.getProject());
    bean.setFeed(yaml.getFeed());
    bean.setPackageId(yaml.getPackageId());
    bean.setPackageName(yaml.getPackageName());
  }

  @Override
  protected AzureArtifactsArtifactStream getNewArtifactStreamObject() {
    return new AzureArtifactsArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
