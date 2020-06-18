package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "values", "deeplinkMetadata", "deeplinkUrl"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "TimeSeriesRecordKeys")
@Entity(value = "timeSeriesRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesRecord implements UuidAware, CreatedAtAware, AccountAccess, PersistentEntity {
  @Id private String uuid;

  @Indexed private String accountId;
  @Indexed private String cvConfigId;
  @Indexed private String host;
  @Indexed private String metricName;
  private long hourStartBoundary;
  private long createdAt;
  @Default private Set<TimeSeriesGroupValue> timeSeriesGroupValues = new HashSet<>();

  @JsonIgnore
  @SchemaIgnore
  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TimeSeriesValueKeys")
  @EqualsAndHashCode(of = {"groupName", "timeStamp"})
  public static class TimeSeriesGroupValue {
    private String groupName;
    private long timeStamp;
    private double metricValue;
  }
}
