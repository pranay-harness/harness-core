package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.EcrArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class EcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<EcrArtifactStreamYaml, EcrArtifactStream> {
  @Override
  public EcrArtifactStreamYaml toYaml(EcrArtifactStream bean, String appId) {
    EcrArtifactStreamYaml yaml = EcrArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setImageName(bean.getImageName());
    yaml.setRegion(bean.getRegion());
    return yaml;
  }

  @Override
  protected EcrArtifactStream getNewArtifactStreamObject() {
    return new EcrArtifactStream();
  }

  @Override
  protected void toBean(EcrArtifactStream bean, ChangeContext<EcrArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    EcrArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setImageName(yaml.getImageName());
    bean.setRegion(yaml.getRegion());
  }

  @Override
  public Class getYamlClass() {
    return EcrArtifactStreamYaml.class;
  }
}
