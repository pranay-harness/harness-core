package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 10/09/17
 */
public class EcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, EcrArtifactStream> {
  public Yaml toYaml(EcrArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setImageName(bean.getImageName());
    yaml.setRegion(bean.getRegion());
    return yaml;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getImageName()) || isEmpty(artifactStreamYaml.getRegion())
        || isEmpty(artifactStreamYaml.getServerName()));
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
