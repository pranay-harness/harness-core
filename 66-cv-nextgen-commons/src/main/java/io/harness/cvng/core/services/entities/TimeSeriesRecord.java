package io.harness.cvng.core.services.entities;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.GoogleDataStoreAware.addFieldIfNotEmpty;
import static io.harness.persistence.GoogleDataStoreAware.readLong;
import static io.harness.persistence.GoogleDataStoreAware.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.common.hash.Hashing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import java.nio.charset.StandardCharsets;
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
public class TimeSeriesRecord implements GoogleDataStoreAware, UuidAware, CreatedAtAware, AccountAccess {
  @Id private String uuid;

  @Indexed private String accountId;
  @Indexed private String projectId;
  @Indexed private String cvConfigId;
  @NotEmpty private long timestamp;
  private String host;
  private long createdAt;
  @Transient @Default private Set<TimeSeriesGroupValue> values = new HashSet<>();

  @JsonIgnore
  @SchemaIgnore
  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TimeSeriesGroupValueKeys")
  @EqualsAndHashCode(of = {"groupName"})
  public static class TimeSeriesGroupValue {
    private String groupName;
    private Set<TimeSeriesValue> timeSeriesValues;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TimeSeriesValueKeys")
  @EqualsAndHashCode(of = {"metricName"})
  public static class TimeSeriesValue {
    private String metricName;
    private double metricValue;
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(Entity.class).value())
                      .newKey(generateUniqueKey());
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesRecordKeys.accountId, accountId, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesRecordKeys.projectId, projectId, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesRecordKeys.cvConfigId, cvConfigId, false);
    recordBuilder.set(TimeSeriesRecordKeys.timestamp, timestamp);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesRecordKeys.host, host, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesRecordKeys.createdAt, createdAt, true);

    if (validUntil == null) {
      validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
    }
    recordBuilder.set(TimeSeriesRecordKeys.validUntil, validUntil.getTime());

    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    return TimeSeriesRecord.builder()
        .accountId(readString(entity, TimeSeriesRecordKeys.accountId))
        .projectId(readString(entity, TimeSeriesRecordKeys.projectId))
        .cvConfigId(readString(entity, TimeSeriesRecordKeys.cvConfigId))
        .timestamp(readLong(entity, TimeSeriesRecordKeys.timestamp))
        .host(readString(entity, TimeSeriesRecordKeys.host))
        .createdAt(readLong(entity, TimeSeriesRecordKeys.createdAt))
        .build();
  }

  private String generateUniqueKey() {
    StringBuilder keyBuilder = new StringBuilder();
    appendIfNecessary(keyBuilder, accountId);
    appendIfNecessary(keyBuilder, projectId);
    appendIfNecessary(keyBuilder, cvConfigId);
    keyBuilder.append(KEY_SEPARATOR).append(timestamp);
    appendIfNecessary(keyBuilder, host);
    return Hashing.sha256().hashString(keyBuilder.toString(), StandardCharsets.UTF_8).toString();
  }

  private void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (isNotEmpty(value)) {
      keyBuilder.append(KEY_SEPARATOR).append(value);
    }
  }
}
