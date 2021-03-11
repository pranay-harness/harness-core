package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class AmazonS3ArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<AmazonS3ArtifactStreamYaml, AmazonS3ArtifactStream> {
  @Override
  public AmazonS3ArtifactStreamYaml toYaml(AmazonS3ArtifactStream bean, String appId) {
    AmazonS3ArtifactStreamYaml yaml = AmazonS3ArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setBucketName(bean.getJobname());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return AmazonS3ArtifactStreamYaml.class;
  }

  @Override
  protected AmazonS3ArtifactStream getNewArtifactStreamObject() {
    return new AmazonS3ArtifactStream();
  }

  @Override
  protected void toBean(
      AmazonS3ArtifactStream bean, ChangeContext<AmazonS3ArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    AmazonS3ArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getBucketName());
  }
}
