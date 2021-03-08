package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.ACR;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AcrArtifactStreamYaml extends ArtifactStreamYaml {
  private String subscriptionId;
  private String registryName;
  private String registryHostName;
  private String repositoryName;

  @lombok.Builder
  public AcrArtifactStreamYaml(String harnessApiVersion, String serverName, boolean metadataOnly, String subscriptionId,
      String registryName, String registryHostName, String repositoryName) {
    super(ACR.name(), harnessApiVersion, serverName);
    this.subscriptionId = subscriptionId;
    this.registryName = registryName;
    this.registryHostName = registryHostName;
    this.repositoryName = repositoryName;
  }
}
