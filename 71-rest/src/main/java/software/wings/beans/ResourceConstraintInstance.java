package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Entity(value = "resourceConstraintInstances", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Indexes(@Index(options = @IndexOptions(unique = true, name = "uniqueUnitOrder"),
    fields = { @Field("resourceConstraintId")
               , @Field("resourceUnit"), @Field("order") }))
@FieldNameConstants(innerTypeName = "ResourceConstraintInstanceKeys")
public class ResourceConstraintInstance implements PersistentEntity, UuidAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;

  private String accountId;

  private String resourceConstraintId;
  private String resourceUnit;
  private int order;

  @Indexed private String state;
  private int permits;

  private String releaseEntityType;
  private String releaseEntityId;

  private long acquiredAt;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
