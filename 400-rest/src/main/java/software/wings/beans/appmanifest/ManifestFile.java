package software.wings.beans.appmanifest;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ManifestFileKeys")
@EqualsAndHashCode(callSuper = false)
@Entity("manifestFile")
@HarnessEntity(exportable = true)
public class ManifestFile extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("manifestFileIdx")
                 .unique(true)
                 .field(ManifestFileKeys.applicationManifestId)
                 .field(ManifestFileKeys.fileName)
                 .build())
        .build();
  }

  public static final String VALUES_YAML_KEY = "values.yaml";

  @NotEmpty String fileName;
  private String fileContent;
  private String applicationManifestId;
  @FdIndex private String accountId;

  public ManifestFile cloneInternal() {
    ManifestFile manifestFile = ManifestFile.builder().fileName(this.fileName).fileContent(this.fileContent).build();
    manifestFile.setAppId(this.appId);
    manifestFile.setAccountId(this.accountId);
    return manifestFile;
  }
}
