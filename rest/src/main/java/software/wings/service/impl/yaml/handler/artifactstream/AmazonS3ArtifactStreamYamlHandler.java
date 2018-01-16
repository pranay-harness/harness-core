package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class AmazonS3ArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, AmazonS3ArtifactStream> {
  @Override
  public Yaml toYaml(AmazonS3ArtifactStream bean, String appId) {
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
  protected AmazonS3ArtifactStream getNewArtifactStreamObject() {
    return new AmazonS3ArtifactStream();
  }

  protected void toBean(AmazonS3ArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getBucketName());
  }
}
