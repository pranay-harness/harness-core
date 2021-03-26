package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.AZURE_ARTIFACTS;

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
public class AzureArtifactsArtifactStreamYaml extends ArtifactStreamYaml {
  private String packageType;
  private String project;
  private String feed;
  private String packageId;
  private String packageName;

  @Builder
  public AzureArtifactsArtifactStreamYaml(String harnessApiVersion, String serverName, String packageType,
      String project, String feed, String packageId, String packageName) {
    super(AZURE_ARTIFACTS.name(), harnessApiVersion, serverName);
    this.packageType = packageType;
    this.project = project;
    this.feed = feed;
    this.packageId = packageId;
    this.packageName = packageName;
  }
}
