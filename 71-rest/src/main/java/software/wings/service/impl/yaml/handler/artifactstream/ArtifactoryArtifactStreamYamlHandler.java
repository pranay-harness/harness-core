package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class ArtifactoryArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, ArtifactoryArtifactStream> {
  @Override
  public Yaml toYaml(ArtifactoryArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setArtifactPattern(bean.getArtifactPattern());
    yaml.setGroupId(bean.getGroupId());
    yaml.setImageName(bean.getImageName());
    yaml.setRepositoryName(bean.getJobname());
    yaml.setRepositoryType(bean.getRepositoryType());
    yaml.setDockerRepositoryServer(bean.getDockerRepositoryServer());
    return yaml;
  }

  protected void toBean(ArtifactoryArtifactStream artifactStream, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(artifactStream, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    artifactStream.setArtifactPaths(yaml.getArtifactPaths());
    artifactStream.setArtifactPattern(yaml.getArtifactPattern());
    artifactStream.setGroupId(yaml.getGroupId());
    artifactStream.setImageName(yaml.getImageName());
    artifactStream.setJobname(yaml.getRepositoryName());
    artifactStream.setRepositoryType(yaml.getRepositoryType());
    artifactStream.setDockerRepositoryServer(yaml.getDockerRepositoryServer());
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
