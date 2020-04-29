package software.wings.metrics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.System.currentTimeMillis;
import static software.wings.common.VerificationConstants.CONNECTOR;
import static software.wings.common.VerificationConstants.DEFAULT_GROUP_NAME;
import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readBlob;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.common.base.Preconditions;
import com.google.common.collect.TreeBasedTable;
import com.google.common.hash.Hashing;
import com.google.protobuf.InvalidProtocolBufferException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.exception.WingsException;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.utils.IndexType;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.verification.generated.TimeSeriesMetricRecordProto.MetricDeeplinks;
import software.wings.service.impl.verification.generated.TimeSeriesMetricRecordProto.MetricValues;
import software.wings.service.impl.verification.generated.TimeSeriesMetricRecordProto.TxnMetricValues;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rsingh on 08/30/17.
 */
@Indexes({
  @Index(fields =
      {
        @Field("stateExecutionId"), @Field("groupName"), @Field(value = "dataCollectionMinute", type = IndexType.DESC)
      },
      options = @IndexOptions(name = "stateExIdx"))
  ,
      @Index(fields = {
        @Field("workflowExecutionId")
        , @Field("groupName"), @Field(value = "dataCollectionMinute", type = IndexType.DESC)
      }, options = @IndexOptions(name = "workflowExIdx")), @Index(fields = {
        @Field(value = "dataCollectionMinute", type = IndexType.DESC), @Field("cvConfigId")
      }, options = @IndexOptions(name = "serviceGuardIdx"))
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "values", "deeplinkMetadata", "deeplinkUrl"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "TimeSeriesMetricRecordKeys")
@Entity(value = "timeSeriesMetricRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesDataRecord
    implements GoogleDataStoreAware, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;

  private StateType stateType; // could be null for older values

  private String workflowId;

  private String workflowExecutionId;

  private String serviceId;

  @Indexed private String cvConfigId;

  private String stateExecutionId;

  @NotEmpty private long timeStamp;

  private int dataCollectionMinute;

  private String host;

  private ClusterLevel level;

  private String tag;

  @Default private String groupName = DEFAULT_GROUP_NAME;

  private byte[] valuesBytes;

  @Transient @Default private TreeBasedTable<String, String, Double> values = TreeBasedTable.create();

  @Transient @Default private TreeBasedTable<String, String, String> deeplinkMetadata = TreeBasedTable.create();

  private transient TreeBasedTable<String, String, String> deeplinkUrl;

  private long createdAt;

  private long lastUpdatedAt;

  @Indexed private String accountId;

  @JsonIgnore
  @SchemaIgnore
  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  public void compress() {
    Preconditions.checkNotNull(values);

    final TxnMetricValues.Builder txnMetricBuilder = TxnMetricValues.newBuilder();

    values.rowMap().forEach((txnName, metricMap) -> {
      final MetricValues.Builder metricValueBuilder = MetricValues.newBuilder();
      metricMap.forEach(metricValueBuilder::putMetricValues);
      txnMetricBuilder.putValues(txnName, metricValueBuilder.build());
    });

    if (deeplinkMetadata != null) {
      deeplinkMetadata.rowMap().forEach((txnName, deepLinkMap) -> {
        final MetricDeeplinks.Builder deepLinkBuilder = MetricDeeplinks.newBuilder();
        deepLinkMap.forEach(deepLinkBuilder::putMetricDeeplinks);
        txnMetricBuilder.putDeeplinkMetadata(txnName, deepLinkBuilder.build());
      });
    }

    valuesBytes = txnMetricBuilder.build().toByteArray();
  }

  public void decompress() {
    if (isEmpty(valuesBytes)) {
      return;
    }

    values = TreeBasedTable.create();
    deeplinkMetadata = TreeBasedTable.create();

    try {
      TxnMetricValues txnMetricValues = TxnMetricValues.parseFrom(valuesBytes);
      txnMetricValues.getValuesMap().forEach(
          (txnName, metricValues) -> metricValues.getMetricValuesMap().forEach((metricName, value) -> {
            values.put(txnName, metricName, value);
          }));

      if (isNotEmpty(txnMetricValues.getDeeplinkMetadataMap())) {
        txnMetricValues.getDeeplinkMetadataMap().forEach(
            (txnName, deeplinks) -> deeplinks.getMetricDeeplinksMap().forEach((metricName, deepLink) -> {
              deeplinkMetadata.put(txnName, metricName, deepLink);
            }));
      }

      valuesBytes = null;
    } catch (InvalidProtocolBufferException e) {
      throw new WingsException(e);
    }
  }

  @PrePersist
  public void onSave() {
    final long currentTime = currentTimeMillis();
    if (createdAt == 0) {
      createdAt = currentTime;
    }
    lastUpdatedAt = currentTime;
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    onSave();
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(Entity.class).value())
                      .newKey(generateUniqueKey());
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.stateType, stateType.name(), true);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.workflowId, workflowId, true);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.workflowExecutionId, workflowExecutionId, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.serviceId, serviceId, true);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.cvConfigId, cvConfigId, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.stateExecutionId, stateExecutionId, false);
    recordBuilder.set(TimeSeriesMetricRecordKeys.timeStamp, timeStamp);
    recordBuilder.set(TimeSeriesMetricRecordKeys.dataCollectionMinute, dataCollectionMinute);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.host, host, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.level, level == null ? null : level.name(), false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.tag, tag, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.groupName, groupName, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.accountId, accountId, false);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.createdAt, createdAt, true);
    addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.lastUpdatedAt, lastUpdatedAt, true);

    if (isNotEmpty(valuesBytes)) {
      addFieldIfNotEmpty(recordBuilder, TimeSeriesMetricRecordKeys.valuesBytes, Blob.copyFrom(valuesBytes), true);
    }

    if (validUntil == null) {
      validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
    }
    recordBuilder.set(TimeSeriesMetricRecordKeys.validUntil, validUntil.getTime());

    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final TimeSeriesDataRecord dataRecord =
        TimeSeriesDataRecord.builder()
            .workflowId(readString(entity, TimeSeriesMetricRecordKeys.workflowId))
            .workflowExecutionId(readString(entity, TimeSeriesMetricRecordKeys.workflowExecutionId))
            .serviceId(readString(entity, TimeSeriesMetricRecordKeys.serviceId))
            .cvConfigId(readString(entity, TimeSeriesMetricRecordKeys.cvConfigId))
            .stateExecutionId(readString(entity, TimeSeriesMetricRecordKeys.stateExecutionId))
            .accountId(readString(entity, TimeSeriesMetricRecordKeys.accountId))
            .timeStamp(readLong(entity, TimeSeriesMetricRecordKeys.timeStamp))
            .dataCollectionMinute((int) readLong(entity, TimeSeriesMetricRecordKeys.dataCollectionMinute))
            .host(readString(entity, TimeSeriesMetricRecordKeys.host))
            .tag(readString(entity, TimeSeriesMetricRecordKeys.tag))
            .groupName(readString(entity, TimeSeriesMetricRecordKeys.groupName))
            .valuesBytes(readBlob(entity, TimeSeriesMetricRecordKeys.valuesBytes))
            .createdAt(readLong(entity, TimeSeriesMetricRecordKeys.createdAt))
            .lastUpdatedAt(readLong(entity, TimeSeriesMetricRecordKeys.lastUpdatedAt))
            .build();

    final String level = readString(entity, TimeSeriesMetricRecordKeys.level);
    if (isNotEmpty(level)) {
      dataRecord.setLevel(ClusterLevel.valueOf(level));
    }

    final String stateType = readString(entity, TimeSeriesMetricRecordKeys.stateType);
    if (isNotEmpty(stateType)) {
      dataRecord.setStateType(StateType.valueOf(stateType));
    }

    return dataRecord;
  }

  private String generateUniqueKey() {
    StringBuilder keyBuilder = new StringBuilder();
    appendIfNecessary(keyBuilder, host);
    keyBuilder.append(CONNECTOR).append(timeStamp);
    appendIfNecessary(keyBuilder, workflowExecutionId);
    appendIfNecessary(keyBuilder, stateExecutionId);
    appendIfNecessary(keyBuilder, serviceId);
    appendIfNecessary(keyBuilder, workflowId);
    appendIfNecessary(keyBuilder, stateType.name());
    appendIfNecessary(keyBuilder, groupName);
    return Hashing.sha256().hashString(keyBuilder.toString(), StandardCharsets.UTF_8).toString();
  }

  private void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (isNotEmpty(value)) {
      keyBuilder.append(CONNECTOR).append(value);
    }
  }

  public static List<TimeSeriesDataRecord> getTimeSeriesDataRecordsFromNewRelicDataRecords(
      List<NewRelicMetricDataRecord> metricData) {
    Map<TimeSeriesDataRecord, TimeSeriesDataRecord> timeSeriesDataRecords = new HashMap();
    metricData.forEach(metric -> {
      TimeSeriesDataRecord timeSeriesDataRecord = TimeSeriesDataRecord.builder()
                                                      .stateType(metric.getStateType())
                                                      .workflowId(metric.getWorkflowId())
                                                      .workflowExecutionId(metric.getWorkflowExecutionId())
                                                      .serviceId(metric.getServiceId())
                                                      .cvConfigId(metric.getCvConfigId())
                                                      .accountId(metric.getAccountId())
                                                      .stateExecutionId(metric.getStateExecutionId())
                                                      .groupName(metric.getGroupName())
                                                      .timeStamp(metric.getTimeStamp())
                                                      .dataCollectionMinute(metric.getDataCollectionMinute())
                                                      .host(metric.getHost())
                                                      .level(metric.getLevel())
                                                      .tag(metric.getTag())
                                                      .createdAt(metric.getCreatedAt())
                                                      .lastUpdatedAt(metric.getLastUpdatedAt())
                                                      .build();

      TreeBasedTable<String, String, Double> values = TreeBasedTable.create();
      TreeBasedTable<String, String, String> deeplinkMetadata = TreeBasedTable.create();
      metric.getValues().forEach((metricName, value) -> values.put(metric.getName(), metricName, value));
      if (isNotEmpty(metric.getDeeplinkMetadata())) {
        metric.getDeeplinkMetadata().forEach(
            (metricName, deepLink) -> deeplinkMetadata.put(metric.getName(), metricName, deepLink));
      }
      if (timeSeriesDataRecords.containsKey(timeSeriesDataRecord)) {
        timeSeriesDataRecords.get(timeSeriesDataRecord).getValues().putAll(values);
        timeSeriesDataRecords.get(timeSeriesDataRecord).getDeeplinkMetadata().putAll(deeplinkMetadata);
      } else {
        timeSeriesDataRecord.setValues(values);
        timeSeriesDataRecord.setDeeplinkMetadata(deeplinkMetadata);
        timeSeriesDataRecords.put(timeSeriesDataRecord, timeSeriesDataRecord);
      }
    });
    return new ArrayList<>(timeSeriesDataRecords.values());
  }

  public static List<NewRelicMetricDataRecord> getNewRelicDataRecordsFromTimeSeriesDataRecords(
      List<TimeSeriesDataRecord> metricData) {
    metricData.forEach(TimeSeriesDataRecord::decompress);
    List<NewRelicMetricDataRecord> newRelicRecords = new ArrayList<>();
    metricData.forEach(metric -> {
      TreeBasedTable<String, String, Double> values = metric.getValues();
      TreeBasedTable<String, String, String> deeplinkMetadata = metric.getDeeplinkMetadata();

      if (metric.getLevel() != null) {
        NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                                .name(VerificationConstants.HEARTBEAT_METRIC_NAME)
                                                                .stateType(metric.getStateType())
                                                                .workflowId(metric.getWorkflowId())
                                                                .workflowExecutionId(metric.getWorkflowExecutionId())
                                                                .serviceId(metric.getServiceId())
                                                                .cvConfigId(metric.getCvConfigId())
                                                                .accountId(metric.getAccountId())
                                                                .stateExecutionId(metric.getStateExecutionId())
                                                                .groupName(metric.getGroupName())
                                                                .timeStamp(metric.getTimeStamp())
                                                                .dataCollectionMinute(metric.getDataCollectionMinute())
                                                                .host(metric.getHost())
                                                                .level(metric.getLevel())
                                                                .tag(metric.getTag())
                                                                .createdAt(metric.getCreatedAt())
                                                                .lastUpdatedAt(metric.getLastUpdatedAt())
                                                                .build();
        newRelicRecords.add(newRelicMetricDataRecord);
      } else {
        Set<String> names = values.rowKeySet();
        names.forEach(name -> {
          NewRelicMetricDataRecord newRelicMetricDataRecord =
              NewRelicMetricDataRecord.builder()
                  .name(name)
                  .stateType(metric.getStateType())
                  .workflowId(metric.getWorkflowId())
                  .workflowExecutionId(metric.getWorkflowExecutionId())
                  .serviceId(metric.getServiceId())
                  .cvConfigId(metric.getCvConfigId())
                  .accountId(metric.getAccountId())
                  .stateExecutionId(metric.getStateExecutionId())
                  .groupName(metric.getGroupName())
                  .timeStamp(metric.getTimeStamp())
                  .dataCollectionMinute(metric.getDataCollectionMinute())
                  .host(metric.getHost())
                  .level(metric.getLevel())
                  .tag(metric.getTag())
                  .values(values.row(name))
                  .deeplinkMetadata(deeplinkMetadata.row(name))
                  .build();
          newRelicRecords.add(newRelicMetricDataRecord);
        });
      }
    });
    return newRelicRecords;
  }
}
