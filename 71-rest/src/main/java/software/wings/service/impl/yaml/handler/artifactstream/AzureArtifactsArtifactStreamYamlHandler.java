package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

@Singleton
public class AzureArtifactsArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<Yaml, AzureArtifactsArtifactStream> {
  @Override
  public Yaml toYaml(AzureArtifactsArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setProtocolType(bean.getProtocolType());
    yaml.setProject(bean.getProject());
    yaml.setFeed(bean.getFeed());
    yaml.setPackageId(bean.getPackageId());
    yaml.setPackageName(bean.getPackageName());
    return yaml;
  }

  @Override
  protected void toBean(AzureArtifactsArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setProtocolType(yaml.getProtocolType());
    bean.setProject(yaml.getProject());
    bean.setFeed(yaml.getFeed());
    bean.setPackageId(yaml.getPackageId());
    bean.setPackageName(yaml.getPackageName());
  }

  @Override
  protected AzureArtifactsArtifactStream getNewArtifactStreamObject() {
    return new AzureArtifactsArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
