package software.wings.beans.yaml;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Date;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("yamlFilePath") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
@FieldNameConstants(innerTypeName = "YamlSuccessfulChangeKeys")
@Entity(value = "yamlSuccessfulChange", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class YamlSuccessfulChange implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                                             UpdatedAtAware, UpdatedByAware, AccountAccess {
  @Id @NotNull(groups = {Update.class}) private String uuid;
  private String accountId;
  private String yamlFilePath;
  private Long changeRequestTS;
  private Long changeProcessedTS;
  private String changeSource;
  private SuccessfulChangeDetail changeDetail;
  private EmbeddedUser createdBy;
  private long createdAt;
  private EmbeddedUser lastUpdatedBy;
  private long lastUpdatedAt;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 24 * 60 * 60)) @Default private Date validUntil = new Date();

  public enum ChangeSource { GIT, HARNESS }
}
