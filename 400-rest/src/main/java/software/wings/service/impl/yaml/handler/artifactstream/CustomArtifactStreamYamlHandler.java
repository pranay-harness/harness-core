package software.wings.service.impl.yaml.handler.artifactstream;

import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@Singleton
public class CustomArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<CustomArtifactStreamYaml, CustomArtifactStream> {
  @Override
  protected CustomArtifactStream getNewArtifactStreamObject() {
    return new CustomArtifactStream();
  }

  @Override
  public CustomArtifactStreamYaml toYaml(CustomArtifactStream bean, String appId) {
    CustomArtifactStreamYaml yaml = CustomArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    if (bean.getTemplateUuid() == null) {
      yaml.setScripts(bean.getScripts());
    }
    yaml.setDelegateTags(bean.getTags());
    return yaml;
  }

  @Override
  protected void toBean(
      CustomArtifactStream bean, ChangeContext<CustomArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    CustomArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setScripts(yaml.getScripts());
    bean.setTags(yaml.getDelegateTags());
  }

  @Override
  public Class getYamlClass() {
    return CustomArtifactStreamYaml.class;
  }
}
