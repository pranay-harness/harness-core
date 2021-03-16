package software.wings.beans.artifact;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ArtifactStreamYaml extends BaseEntityYaml {
  private String serverName;
  private String templateUri;
  private List<NameValuePair> templateVariables;

  public ArtifactStreamYaml(String type, String harnessApiVersion, String serverName) {
    super(type, harnessApiVersion);
    this.serverName = serverName;
  }

  public ArtifactStreamYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }

  public ArtifactStreamYaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly,
      String templateUri, List<NameValuePair> templateVariables) {
    super(type, harnessApiVersion);
    this.serverName = serverName;
    this.templateUri = templateUri;
    this.templateVariables = templateVariables;
  }
}
