package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.AZURE_MACHINE_IMAGE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureMachineImageArtifactStreamYaml extends ArtifactStreamYaml {
  private AzureMachineImageArtifactStream.ImageType imageType;
  private String subscriptionId;
  private AzureMachineImageArtifactStream.ImageDefinition imageDefinition;

  @Builder
  public AzureMachineImageArtifactStreamYaml(String harnessApiVersion, String serverName,
      AzureMachineImageArtifactStream.ImageType imageType, String subscriptionId,
      AzureMachineImageArtifactStream.ImageDefinition imageDefinition) {
    super(AZURE_MACHINE_IMAGE.name(), harnessApiVersion, serverName);
    this.imageType = imageType;
    this.subscriptionId = subscriptionId;
    this.imageDefinition = imageDefinition;
  }
}
