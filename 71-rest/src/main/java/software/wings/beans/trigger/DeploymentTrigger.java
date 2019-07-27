package software.wings.beans.trigger;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */

@Entity(value = "deploymentTriggers", noClassnameStored = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Indexes(@Index(
    options = @IndexOptions(name = "uniqueTriggerIdx", unique = true), fields = { @Field("appId")
                                                                                  , @Field("name") }))
@HarnessExportableEntity
@FieldNameConstants(innerTypeName = "DeploymentTriggerKeys")
public class DeploymentTrigger
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id @NotNull(groups = {DeploymentTrigger.class}) @SchemaIgnore private String uuid;
  @NotNull protected String appId;
  @Indexed protected String accountId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @EntityName @NotEmpty @Trimmed private String name;
  private String description;

  private Action action;
  @NotNull private Condition condition;
}
