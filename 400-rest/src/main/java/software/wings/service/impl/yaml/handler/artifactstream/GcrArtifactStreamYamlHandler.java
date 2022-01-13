/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class GcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, GcrArtifactStream> {
  @Override
  public Yaml toYaml(GcrArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDockerImageName(bean.getDockerImageName());
    yaml.setRegistryHostName(bean.getRegistryHostName());
    return yaml;
  }

  @Override
  protected void toBean(GcrArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setDockerImageName(yaml.getDockerImageName());
    bean.setRegistryHostName(yaml.getRegistryHostName());
  }

  @Override
  protected GcrArtifactStream getNewArtifactStreamObject() {
    return new GcrArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
