package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TimeSeriesThresholdKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity(value = "timeSeriesThresholds", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class TimeSeriesThreshold implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @NotNull @FdIndex private String accountId;
  @NotNull @FdIndex private String projectIdentifier;
  @NotNull @FdIndex private DataSourceType dataSourceType;
  @NotNull @FdIndex private String metricPackIdentifier;
  @NotNull private String metricName;
  private String metricGroupName;
  @NotNull private TimeSeriesThresholdActionType action;
  @NotNull private TimeSeriesThresholdCriteria criteria;
}
