package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.AzureMachineImageArtifactStream;
import software.wings.beans.artifact.AzureMachineImageArtifactStreamYml;
import software.wings.beans.yaml.ChangeContext;

@OwnedBy(CDC)
public class AzureMachineImageArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<AzureMachineImageArtifactStreamYml, AzureMachineImageArtifactStream> {
  @Override
  protected AzureMachineImageArtifactStream getNewArtifactStreamObject() {
    return new AzureMachineImageArtifactStream();
  }

  @Override
  public AzureMachineImageArtifactStreamYml toYaml(AzureMachineImageArtifactStream bean, String appId) {
    AzureMachineImageArtifactStreamYml yml = AzureMachineImageArtifactStreamYml.builder().build();
    super.toYaml(yml, bean);
    yml.setSubscriptionId(bean.getSubscriptionId());
    yml.setImageType(bean.getImageType());
    yml.setImageDefinition(bean.getImageDefinition());
    return yml;
  }

  @Override
  public Class getYamlClass() {
    return AzureMachineImageArtifactStreamYml.class;
  }

  @Override
  protected void toBean(AzureMachineImageArtifactStream bean,
      ChangeContext<AzureMachineImageArtifactStreamYml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    AzureMachineImageArtifactStreamYml yml = changeContext.getYaml();
    bean.setImageType(yml.getImageType());
    bean.setImageDefinition(yml.getImageDefinition());
    bean.setSubscriptionId(yml.getSubscriptionId());
  }
}
