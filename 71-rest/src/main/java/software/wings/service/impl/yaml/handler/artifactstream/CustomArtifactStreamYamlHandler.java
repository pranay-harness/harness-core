package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

@Singleton
public class CustomArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<CustomArtifactStream.Yaml, CustomArtifactStream> {
  @Override
  protected CustomArtifactStream getNewArtifactStreamObject() {
    return new CustomArtifactStream();
  }

  @Override
  public Yaml toYaml(CustomArtifactStream bean, String appId) {
    CustomArtifactStream.Yaml yaml = CustomArtifactStream.Yaml.builder().build();
    super.toYaml(yaml, bean);
    if (bean.getTemplateUuid() == null) {
      yaml.setScripts(bean.getScripts());
    }
    yaml.setDelegateTags(bean.getTags());
    return yaml;
  }

  @Override
  protected void toBean(CustomArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setScripts(yaml.getScripts());
    bean.setTags(yaml.getDelegateTags());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
