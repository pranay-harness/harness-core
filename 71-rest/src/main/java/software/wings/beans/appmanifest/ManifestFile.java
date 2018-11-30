package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.yaml.YamlType;
import software.wings.yaml.BaseEntityYaml;

@Entity("manifestFile")
@Data
@Builder
public class ManifestFile extends Base {
  public static final String APP_MANIFEST_FILE_NAME = "fileName";
  @NotEmpty String fileName;
  private String fileContent;
  @Indexed private String applicationManifestId;

  public ManifestFile cloneInternal() {
    ManifestFile manifestFile = ManifestFile.builder().fileName(this.fileName).fileContent(this.fileContent).build();
    manifestFile.setAppId(this.appId);
    return manifestFile;
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String fileContent;

    @Builder
    public Yaml(String type, String harnessApiVersion, String fileContent) {
      super(YamlType.MANIFEST_FILE.name(), harnessApiVersion);
      this.fileContent = fileContent;
    }
  }
}
