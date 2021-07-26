package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;

import software.wings.beans.entityinterface.ApplicationAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "CgEventConfigKeys")
@Entity(value = "cgEventConfig", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class CgEventConfig
    extends EventConfig implements PersistentEntity, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware,
                                   ApplicationAccess, NameAccess, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("yaml")
                 .unique(true)
                 .field(CgEventConfigKeys.appId)
                 .field(CgEventConfigKeys.name)
                 .build())
        .build();
  }

  @Trimmed(message = "Event Config Name should not contain leading and trailing spaces")
  @NotBlank
  @EntityName
  private String name;
  @NotNull private WebHookEventConfig config;
  private CgEventRule rule;
  @FdIndex private String accountId;
  private List<String> delegateSelectors;
  private boolean enabled;

  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;

  @JsonIgnore @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAccountId() {
    return accountId;
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public void setCreatedBy(EmbeddedUser createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public EmbeddedUser getCreatedBy() {
    return createdBy;
  }

  @Override
  public void setLastUpdatedAt(long lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  @Override
  public long getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  @Override
  public void setLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Override
  public EmbeddedUser getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @UtilityClass
  public static final class CgEventConfigKeys {
    public static final String uuid = "uuid";
  }
}
