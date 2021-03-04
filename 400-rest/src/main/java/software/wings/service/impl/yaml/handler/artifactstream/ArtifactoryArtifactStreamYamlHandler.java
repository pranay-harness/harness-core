package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.RepositoryType;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class ArtifactoryArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<ArtifactoryArtifactStreamYaml, ArtifactoryArtifactStream> {
  @Override
  public ArtifactoryArtifactStreamYaml toYaml(ArtifactoryArtifactStream bean, String appId) {
    ArtifactoryArtifactStreamYaml yaml = ArtifactoryArtifactStreamYaml.builder().build();
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
  protected void toBean(ArtifactoryArtifactStream artifactStream,
      ChangeContext<ArtifactoryArtifactStreamYaml> changeContext, String appId) {
    super.toBean(artifactStream, changeContext, appId);
    ArtifactoryArtifactStreamYaml yaml = changeContext.getYaml();
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
    return ArtifactoryArtifactStreamYaml.class;
  }
}
