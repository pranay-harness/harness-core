package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.BambooArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class BambooArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<BambooArtifactStreamYaml, BambooArtifactStream> {
  @Override
  public BambooArtifactStreamYaml toYaml(BambooArtifactStream bean, String appId) {
    BambooArtifactStreamYaml yaml = BambooArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setPlanName(bean.getJobname());
    yaml.setMetadataOnly(bean.isMetadataOnly());
    return yaml;
  }

  @Override
  protected void toBean(
      BambooArtifactStream bean, ChangeContext<BambooArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    BambooArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getPlanName());
    bean.setMetadataOnly(yaml.isMetadataOnly());
  }

  @Override
  protected BambooArtifactStream getNewArtifactStreamObject() {
    return new BambooArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return BambooArtifactStreamYaml.class;
  }
}
