package io.harness.cvng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.cvnglog.CVNGLogType;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "CVNGLogKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "cvngLogs")
@HarnessEntity(exportable = true)
@SuperBuilder
public abstract class CVNGLog implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess, UpdatedAtAware {
  @Id private String uuid;
  @NonNull @FdIndex private String accountId;
  @NotEmpty private String traceableId;
  private long createdAt;
  private Instant startTime;
  private Instant endTime;
  private TraceableType traceableType;
  private long lastUpdatedAt;
  @JsonIgnore
  @SchemaIgnore
  @Builder.Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  public abstract CVNGLogType getType();
}
