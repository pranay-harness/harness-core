package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class AzureArtifactsArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<AzureArtifactsArtifactStreamYaml, AzureArtifactsArtifactStream> {
  @Override
  public AzureArtifactsArtifactStreamYaml toYaml(AzureArtifactsArtifactStream bean, String appId) {
    AzureArtifactsArtifactStreamYaml yaml = AzureArtifactsArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setPackageType(bean.getProtocolType());
    yaml.setProject(bean.getProject());
    yaml.setFeed(bean.getFeed());
    yaml.setPackageId(bean.getPackageId());
    yaml.setPackageName(bean.getPackageName());
    return yaml;
  }

  @Override
  protected void toBean(
      AzureArtifactsArtifactStream bean, ChangeContext<AzureArtifactsArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    AzureArtifactsArtifactStreamYaml yaml = changeContext.getYaml();
    bean.setProtocolType(yaml.getPackageType());
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
    return AzureArtifactsArtifactStreamYaml.class;
  }
}
