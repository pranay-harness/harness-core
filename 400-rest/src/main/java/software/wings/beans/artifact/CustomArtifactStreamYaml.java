package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomArtifactStreamYaml extends ArtifactStreamYaml {
  @NotNull private List<CustomArtifactStream.Script> scripts = new ArrayList<>();
  private List<String> delegateTags = new ArrayList<>();

  @lombok.Builder
  public CustomArtifactStreamYaml(String harnessApiVersion, String serverName,
      List<CustomArtifactStream.Script> scripts, List<String> delegateTags) {
    super(CUSTOM.name(), harnessApiVersion, serverName);
    this.scripts = scripts;
    this.delegateTags = delegateTags;
  }
}
