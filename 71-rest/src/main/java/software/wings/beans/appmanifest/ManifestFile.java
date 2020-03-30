package software.wings.beans.appmanifest;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.yaml.BaseEntityYaml;

@Indexes(@Index(options = @IndexOptions(name = "manifestFileIdx", unique = true),
    fields = { @Field("applicationManifestId")
               , @Field("fileName") }))
@Data
@Builder
@FieldNameConstants(innerTypeName = "ManifestFileKeys")
@EqualsAndHashCode(callSuper = false)
@Entity("manifestFile")
@HarnessEntity(exportable = true)
public class ManifestFile extends Base {
  public static final String VALUES_YAML_KEY = "values.yaml";

  @NotEmpty String fileName;
  private String fileContent;
  private String applicationManifestId;
  @Indexed private String accountId;

  public ManifestFile cloneInternal() {
    ManifestFile manifestFile = ManifestFile.builder().fileName(this.fileName).fileContent(this.fileContent).build();
    manifestFile.setAppId(this.appId);
    manifestFile.setAccountId(this.accountId);
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
