package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class BambooArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, BambooArtifactStream> {
  public Yaml toYaml(BambooArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setPlanName(bean.getJobname());
    return yaml;
  }

  protected void toBean(BambooArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getPlanName());
  }

  @Override
  protected BambooArtifactStream getNewArtifactStreamObject() {
    return new BambooArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
