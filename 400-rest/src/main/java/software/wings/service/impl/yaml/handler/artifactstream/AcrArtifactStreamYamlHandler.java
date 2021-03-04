package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AcrArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class AcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<AcrArtifactStreamYaml, AcrArtifactStream> {
  @Override
  public AcrArtifactStreamYaml toYaml(AcrArtifactStream bean, String appId) {
    AcrArtifactStreamYaml yaml = AcrArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setSubscriptionId(bean.getSubscriptionId());
    yaml.setRegistryName(bean.getRegistryName());
    yaml.setRegistryHostName(bean.getRegistryHostName());
    yaml.setRepositoryName(bean.getRepositoryName());
    return yaml;
  }

  @Override
  protected void toBean(AcrArtifactStream bean, ChangeContext<AcrArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    AcrArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setSubscriptionId(yaml.getSubscriptionId());
    bean.setRegistryName(yaml.getRegistryName());
    bean.setRegistryHostName(yaml.getRegistryHostName());
    bean.setRepositoryName(yaml.getRepositoryName());
  }

  @Override
  protected AcrArtifactStream getNewArtifactStreamObject() {
    return new AcrArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return AcrArtifactStreamYaml.class;
  }
}
