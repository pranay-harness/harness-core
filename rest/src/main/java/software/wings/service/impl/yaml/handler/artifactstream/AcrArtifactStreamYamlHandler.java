package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AcrArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@Singleton
public class AcrArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, AcrArtifactStream> {
  public Yaml toYaml(AcrArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setSubscriptionId(bean.getSubscriptionId());
    yaml.setRegistryName(bean.getRegistryName());
    yaml.setRepositoryName(bean.getRepositoryName());
    return yaml;
  }

  protected void toBean(AcrArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setSubscriptionId(yaml.getSubscriptionId());
    bean.setRegistryName(yaml.getRegistryName());
    bean.setRepositoryName(yaml.getRepositoryName());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml artifactStreamYaml = changeContext.getYaml();
    return !(isEmpty(artifactStreamYaml.getSubscriptionId()) || isEmpty(artifactStreamYaml.getRegistryName())
        || isEmpty(artifactStreamYaml.getRepositoryName()) || isEmpty(artifactStreamYaml.getServerName()));
  }

  @Override
  protected AcrArtifactStream getNewArtifactStreamObject() {
    return new AcrArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}