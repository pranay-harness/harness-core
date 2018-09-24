package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class EcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, EcrArtifactStream> {
  public Yaml toYaml(EcrArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setImageName(bean.getImageName());
    yaml.setRegion(bean.getRegion());
    return yaml;
  }

  @Override
  protected EcrArtifactStream getNewArtifactStreamObject() {
    return new EcrArtifactStream();
  }

  protected void toBean(EcrArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setImageName(yaml.getImageName());
    bean.setRegion(yaml.getRegion());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
