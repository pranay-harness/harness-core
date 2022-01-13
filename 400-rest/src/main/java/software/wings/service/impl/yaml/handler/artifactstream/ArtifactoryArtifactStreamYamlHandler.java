/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.RepositoryType;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class ArtifactoryArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, ArtifactoryArtifactStream> {
  @Override
  public Yaml toYaml(ArtifactoryArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    if (isNotEmpty(bean.getArtifactPattern())) {
      yaml.setArtifactPattern(bean.getArtifactPattern());
    } else {
      yaml.setImageName(bean.getImageName());
      yaml.setDockerRepositoryServer(bean.getDockerRepositoryServer());
    }
    yaml.setRepositoryName(bean.getJobname());
    yaml.setRepositoryType(bean.getRepositoryType());
    yaml.setUseDockerFormat(bean.isUseDockerFormat());
    if (!bean.getRepositoryType().equals(RepositoryType.docker.name())) {
      yaml.setMetadataOnly(bean.isMetadataOnly());
    } else {
      yaml.setMetadataOnly(true);
    }
    return yaml;
  }

  @Override
  protected void toBean(ArtifactoryArtifactStream artifactStream, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(artifactStream, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    artifactStream.setArtifactPaths(yaml.getArtifactPaths());
    if (isNotEmpty(yaml.getArtifactPattern())) {
      artifactStream.setArtifactPattern(yaml.getArtifactPattern());
    } else {
      artifactStream.setImageName(yaml.getImageName());
      artifactStream.setDockerRepositoryServer(yaml.getDockerRepositoryServer());
    }
    artifactStream.setJobname(yaml.getRepositoryName());
    artifactStream.setRepositoryType(yaml.getRepositoryType());
    artifactStream.setUseDockerFormat(yaml.isUseDockerFormat());
    if (!yaml.getRepositoryType().equals(RepositoryType.docker.name())) {
      artifactStream.setMetadataOnly(yaml.isMetadataOnly());
    } else {
      artifactStream.setMetadataOnly(true);
    }
  }

  @Override
  protected ArtifactoryArtifactStream getNewArtifactStreamObject() {
    return new ArtifactoryArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
