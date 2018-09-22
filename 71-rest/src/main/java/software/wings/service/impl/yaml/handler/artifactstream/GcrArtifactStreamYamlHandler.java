package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class GcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, GcrArtifactStream> {
  public Yaml toYaml(GcrArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDockerImageName(bean.getDockerImageName());
    yaml.setRegistryHostName(bean.getRegistryHostName());
    return yaml;
  }

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
