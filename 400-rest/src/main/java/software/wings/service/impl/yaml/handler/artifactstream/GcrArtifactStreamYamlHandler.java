package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class GcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<GcrArtifactStreamYaml, GcrArtifactStream> {
  @Override
  public GcrArtifactStreamYaml toYaml(GcrArtifactStream bean, String appId) {
    GcrArtifactStreamYaml yaml = GcrArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDockerImageName(bean.getDockerImageName());
    yaml.setRegistryHostName(bean.getRegistryHostName());
    return yaml;
  }

  @Override
  protected void toBean(GcrArtifactStream bean, ChangeContext<GcrArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    GcrArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setDockerImageName(yaml.getDockerImageName());
    bean.setRegistryHostName(yaml.getRegistryHostName());
  }

  @Override
  protected GcrArtifactStream getNewArtifactStreamObject() {
    return new GcrArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return GcrArtifactStreamYaml.class;
  }
}
