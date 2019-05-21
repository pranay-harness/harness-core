package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.HarnessTag.HarnessTagKeys;

import java.util.Set;
import javax.validation.constraints.NotNull;

@Entity(value = "tags", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@HarnessExportableEntity
@Indexes(@Index(options = @IndexOptions(name = "tagIdx", unique = true),
    fields = { @Field(HarnessTagKeys.accountId)
               , @Field(HarnessTagKeys.key) }))
@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessTagKeys")
public class HarnessTag
    implements PersistentEntity, UuidAware, UpdatedAtAware, UpdatedByAware, CreatedAtAware, CreatedByAware {
  @Id private String uuid;
  @NotEmpty private String accountId;
  private String key;
  private Set<String> allowedValues;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
}
