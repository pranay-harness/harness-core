package io.harness.beans.shared;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.distribution.constraint.Constraint;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ResourceConstraintKeys")
@Entity(value = "resourceConstraint", noClassnameStored = true)
@Document("resourceConstraint")
@TypeAlias("resourceConstraint")
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceConstraint implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                           UpdatedByAware, AccountAccess, ResourceRestraint {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniqueName")
                 .unique(true)
                 .field(ResourceConstraintKeys.accountId)
                 .field(ResourceConstraintKeys.name)
                 .build())
        .build();
  }
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String NAME_KEY = "name";

  @org.springframework.data.annotation.Id @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotEmpty private String accountId;
  @NotEmpty @Trimmed private String name;
  @Min(value = 1) @Max(value = 1000) private int capacity;
  private Constraint.Strategy strategy;
  private boolean harnessOwned;

  @Override
  public String getClaimant() {
    return accountId;
  }
}
