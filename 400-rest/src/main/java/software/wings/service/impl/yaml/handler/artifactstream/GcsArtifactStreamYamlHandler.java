package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.beans.artifact.GcsArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class GcsArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<GcsArtifactStreamYaml, GcsArtifactStream> {
  @Override
  public GcsArtifactStreamYaml toYaml(GcsArtifactStream bean, String appId) {
    GcsArtifactStreamYaml yaml = GcsArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setBucketName(bean.getJobname());
    yaml.setProjectId(bean.getProjectId());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return GcsArtifactStreamYaml.class;
  }

  @Override
  protected GcsArtifactStream getNewArtifactStreamObject() {
    return new GcsArtifactStream();
  }

  @Override
  protected void toBean(GcsArtifactStream bean, ChangeContext<GcsArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    GcsArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getBucketName());
    bean.setProjectId(yaml.getProjectId());
  }
}
