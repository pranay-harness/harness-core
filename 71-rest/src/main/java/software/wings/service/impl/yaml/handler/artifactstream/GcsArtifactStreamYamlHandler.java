package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.beans.artifact.GcsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

@OwnedBy(CDC)
@Singleton
public class GcsArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, GcsArtifactStream> {
  @Override
  public Yaml toYaml(GcsArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setBucketName(bean.getJobname());
    yaml.setProjectId(bean.getProjectId());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected GcsArtifactStream getNewArtifactStreamObject() {
    return new GcsArtifactStream();
  }

  @Override
  protected void toBean(GcsArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getBucketName());
    bean.setProjectId(yaml.getProjectId());
  }
}
