package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.AMI;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePairYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AmiArtifactStreamYaml extends ArtifactStreamYaml {
  private String platform;
  private String region;
  private List<NameValuePairYaml> amiTags = new ArrayList<>();
  private List<NameValuePairYaml> amiFilters = new ArrayList<>();

  @lombok.Builder
  public AmiArtifactStreamYaml(String harnessApiVersion, String serverName, List<NameValuePairYaml> amiTags,
      String region, String platform, List<NameValuePairYaml> amiFilters) {
    super(AMI.name(), harnessApiVersion, serverName);
    this.amiTags = amiTags;
    this.region = region;
    this.platform = platform;
    this.amiFilters = amiFilters;
  }
}
