package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.beans.artifact.GcsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

@Singleton
public class GcsArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, GcsArtifactStream> {
  @Override
  public Yaml toYaml(GcsArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setBucketName(bean.getJobname());
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

  protected void toBean(GcsArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getBucketName());
  }
}
