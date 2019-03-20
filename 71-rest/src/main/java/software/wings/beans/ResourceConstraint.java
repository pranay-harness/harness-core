package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.distribution.constraint.Constraint;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity(value = "resourceConstraint", noClassnameStored = true)
@Indexes(@Index(
    options = @IndexOptions(unique = true, name = "uniqueName"), fields = { @Field("accountId")
                                                                            , @Field("name") }))
@HarnessExportableEntity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ResourceConstraint
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String NAME_KEY = "name";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotEmpty private String accountId;
  @NotEmpty @Trimmed private String name;
  @Min(value = 1) @Max(value = 1000) private int capacity;
  private Constraint.Strategy strategy;

  @Builder
  private ResourceConstraint(String uuid, String accountId, String name, int capacity, Constraint.Strategy strategy) {
    setUuid(uuid);
    this.accountId = accountId;
    this.name = name;
    this.capacity = capacity;
    this.strategy = strategy;
  }
}
