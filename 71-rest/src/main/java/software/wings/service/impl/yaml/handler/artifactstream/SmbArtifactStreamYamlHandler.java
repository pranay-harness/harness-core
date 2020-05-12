package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

@OwnedBy(CDC)
@Singleton
public class SmbArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, SmbArtifactStream> {
  @Override
  public Yaml toYaml(SmbArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected SmbArtifactStream getNewArtifactStreamObject() {
    return new SmbArtifactStream();
  }

  @Override
  protected void toBean(SmbArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
  }
}
