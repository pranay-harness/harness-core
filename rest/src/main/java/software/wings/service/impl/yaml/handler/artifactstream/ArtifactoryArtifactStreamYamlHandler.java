package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
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
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getServerName()) || isEmpty(artifactStreamYaml.getGroupId())
        || isEmpty(artifactStreamYaml.getImageName()));
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
