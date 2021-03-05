package software.wings.beans.appmanifest;

import software.wings.yaml.BaseEntityYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class ManifestFileYaml extends BaseEntityYaml {
  private String fileContent;

  @Builder
  public ManifestFileYaml(String type, String harnessApiVersion, String fileContent) {
    super(type, harnessApiVersion);
    this.fileContent = fileContent;
  }
}
