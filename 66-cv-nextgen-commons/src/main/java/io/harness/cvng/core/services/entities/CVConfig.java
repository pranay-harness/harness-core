package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@FieldNameConstants(innerTypeName = "CVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cvConfigs")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class CVConfig implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  @NotNull private String name;
  private long createdAt;
  private long lastUpdatedAt;
  @NotNull @FdIndex private String accountId;
  @NotNull @FdIndex private String connectorId;
  @NotNull private String serviceIdentifier;
  @NotNull private String envIdentifier;
  @NotNull private String projectIdentifier;
  private String category;
  private String productName;
  private String groupId;
  public abstract DataSourceType getType();
}
