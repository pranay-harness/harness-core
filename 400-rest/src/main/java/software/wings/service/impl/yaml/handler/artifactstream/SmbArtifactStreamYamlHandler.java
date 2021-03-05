package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.beans.artifact.SmbArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class SmbArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<SmbArtifactStreamYaml, SmbArtifactStream> {
  @Override
  public SmbArtifactStreamYaml toYaml(SmbArtifactStream bean, String appId) {
    SmbArtifactStreamYaml yaml = SmbArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return SmbArtifactStreamYaml.class;
  }

  @Override
  protected SmbArtifactStream getNewArtifactStreamObject() {
    return new SmbArtifactStream();
  }

  @Override
  protected void toBean(SmbArtifactStream bean, ChangeContext<SmbArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    SmbArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
  }
}
