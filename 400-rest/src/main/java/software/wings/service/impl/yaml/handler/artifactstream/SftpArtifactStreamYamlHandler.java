package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.SftpArtifactStream;
import software.wings.beans.artifact.SftpArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class SftpArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<SftpArtifactStreamYaml, SftpArtifactStream> {
  @Override
  public SftpArtifactStreamYaml toYaml(SftpArtifactStream bean, String appId) {
    SftpArtifactStreamYaml yaml = SftpArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return SftpArtifactStreamYaml.class;
  }

  @Override
  protected SftpArtifactStream getNewArtifactStreamObject() {
    return new SftpArtifactStream();
  }

  @Override
  protected void toBean(SftpArtifactStream bean, ChangeContext<SftpArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    SftpArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
  }
}
