package software.wings.beans.appmanifest;

import io.harness.annotation.HarnessExportableEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.yaml.BaseEntityYaml;

@Entity("manifestFile")
@HarnessExportableEntity
@Indexes(@Index(options = @IndexOptions(name = "manifestFileIdx", unique = true),
    fields = { @Field("applicationManifestId")
               , @Field("fileName") }))
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ManifestFile extends Base {
  public static final String FILE_NAME_KEY = "fileName";
  public static final String APPLICATION_MANIFEST_ID_KEY = "applicationManifestId";

  @NotEmpty String fileName;
  private String fileContent;
  private String applicationManifestId;

  public ManifestFile cloneInternal() {
    ManifestFile manifestFile = ManifestFile.builder().fileName(this.fileName).fileContent(this.fileContent).build();
    manifestFile.setAppId(this.appId);
    return manifestFile;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class Yaml extends BaseEntityYaml {
    private String fileContent;

    @Builder
    public Yaml(String type, String harnessApiVersion, String fileContent) {
      super(type, harnessApiVersion);
      this.fileContent = fileContent;
    }
  }
}
