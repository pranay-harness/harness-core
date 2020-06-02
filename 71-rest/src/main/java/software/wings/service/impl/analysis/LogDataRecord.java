package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.GoogleDataStoreAware.addFieldIfNotEmpty;
import static io.harness.persistence.GoogleDataStoreAware.readBlob;
import static io.harness.persistence.GoogleDataStoreAware.readLong;
import static io.harness.persistence.GoogleDataStoreAware.readString;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rsingh on 6/20/17.
 */
@Indexes({
  @Index(fields = { @Field("stateExecutionId")
                    , @Field("logCollectionMinute") },
      options = @IndexOptions(name = "stateHostIdx"))
  ,
      @Index(fields = {
        @Field("stateExecutionId")
        , @Field("clusterLevel"), @Field(value = "logCollectionMinute", type = IndexType.DESC), @Field("host")
      }, options = @IndexOptions(name = "stateBumpIdx")), @Index(fields = {
        @Field("workflowExecutionId"), @Field("clusterLevel"), @Field("stateType")
      }, options = @IndexOptions(name = "state_Prev_Ex_Idx")), @Index(fields = {
        @Field("cvConfigId"), @Field(value = "logCollectionMinute", type = IndexType.ASC), @Field("clusterLevel")
      }, options = @IndexOptions(name = "cvRawRecordIdx")), @Index(fields = {
        @Field("cvConfigId")
        , @Field("clusterLevel"), @Field(value = "logCollectionMinute", type = IndexType.DESC), @Field("host"),
            @Field(value = "createdAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "cv_bump_idx"))
})
@Data
@Builder
@FieldNameConstants(innerTypeName = "LogDataRecordKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "logMessage"})
@Entity(value = "logDataRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class LogDataRecord extends Base implements GoogleDataStoreAware, AccountAccess {
  @NotEmpty private StateType stateType;

  @NotEmpty private String workflowId;

  @NotEmpty private String workflowExecutionId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty private String stateExecutionId;

  private String cvConfigId;

  @NotEmpty private String query;

  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty private long timeStamp;

  private String supervisedLabel;
  private int timesLabeled;
  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @NotEmpty private ClusterLevel clusterLevel;
  @NotEmpty private long logCollectionMinute;

  @Indexed private String accountId;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  public static List<LogDataRecord> generateDataRecords(StateType stateType, String applicationId, String cvConfigId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, ClusterLevel heartbeat, List<LogElement> logElements, String accountId) {
    final List<LogDataRecord> records = new ArrayList<>();
    for (LogElement logElement : logElements) {
      final LogDataRecord record = new LogDataRecord();
      record.setStateType(stateType);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setCvConfigId(cvConfigId);
      record.setStateExecutionId(stateExecutionId);
      record.setQuery(logElement.getQuery());
      record.setAppId(applicationId);
      record.setClusterLabel(logElement.getClusterLabel());
      record.setHost(logElement.getHost());
      record.setTimeStamp(logElement.getTimeStamp());
      record.setCount(logElement.getCount());
      record.setLogMessage(logElement.getLogMessage());
      record.setLogMD5Hash(DigestUtils.md5Hex(logElement.getLogMessage()));
      record.setClusterLevel(Integer.parseInt(logElement.getClusterLabel()) < 0 ? heartbeat : clusterLevel);
      record.setServiceId(serviceId);
      record.setLogCollectionMinute(logElement.getLogCollectionMinute());
      record.setAccountId(accountId);
      records.add(record);
    }
    return records;
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(this.getUuid() != null ? this.getUuid() : generateUuid());

    com.google.cloud.datastore.Entity.Builder dataStoreRecordBuilder =
        com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.stateType, stateType.name(), true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "appId", appId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.workflowId, workflowId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.workflowExecutionId, workflowExecutionId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.serviceId, serviceId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.cvConfigId, cvConfigId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.stateExecutionId, stateExecutionId, true);
    dataStoreRecordBuilder.set(LogDataRecordKeys.timeStamp, timeStamp);
    dataStoreRecordBuilder.set(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);

    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.timesLabeled, timesLabeled, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.query, query, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.clusterLabel, clusterLabel, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.logMD5Hash, logMD5Hash, true);

    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.clusterLevel, String.valueOf(clusterLevel), false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.count, count, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.accountId, accountId, false);
    try {
      Blob compressedLog = Blob.copyFrom(compressString(logMessage));
      addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.logMessage, compressedLog, true);
    } catch (Exception ex) {
      return null;
    }
    addFieldIfNotEmpty(dataStoreRecordBuilder, LogDataRecordKeys.host, host, true);

    if (validUntil != null) {
      dataStoreRecordBuilder.set(LogDataRecordKeys.validUntil, validUntil.getTime());
    }

    return dataStoreRecordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final LogDataRecord dataRecord =
        LogDataRecord.builder()
            .stateType(StateType.valueOf(readString(entity, LogDataRecordKeys.stateType)))
            .serviceId(readString(entity, LogDataRecordKeys.serviceId))
            .cvConfigId(readString(entity, LogDataRecordKeys.cvConfigId))
            .query(readString(entity, LogDataRecordKeys.query))
            .clusterLabel(readString(entity, LogDataRecordKeys.clusterLabel))
            .host(readString(entity, LogDataRecordKeys.host))
            .timeStamp(readLong(entity, LogDataRecordKeys.timeStamp))
            .logMD5Hash(readString(entity, LogDataRecordKeys.logMD5Hash))
            .clusterLevel(ClusterLevel.valueOf(readString(entity, LogDataRecordKeys.clusterLevel)))
            .workflowId(readString(entity, LogDataRecordKeys.workflowId))
            .workflowExecutionId(readString(entity, LogDataRecordKeys.workflowExecutionId))
            .stateExecutionId(readString(entity, LogDataRecordKeys.stateExecutionId))
            .validUntil(new Date(readLong(entity, LogDataRecordKeys.validUntil)))
            .accountId(readString(entity, LogDataRecordKeys.accountId))
            .build();

    try {
      dataRecord.setTimesLabeled((int) readLong(entity, LogDataRecordKeys.timesLabeled));
    } catch (ClassCastException ex) {
      String timesLabeledValue = readString(entity, LogDataRecordKeys.timesLabeled);
      if (isNotEmpty(timesLabeledValue)) {
        dataRecord.setTimesLabeled(Integer.parseInt(timesLabeledValue));
      }
    }

    try {
      dataRecord.setCount((int) readLong(entity, LogDataRecordKeys.count));
    } catch (ClassCastException ex) {
      String countVal = readString(entity, LogDataRecordKeys.count);
      if (isNotEmpty(countVal)) {
        dataRecord.setCount(Integer.parseInt(countVal));
      }
    }

    try {
      byte[] byteCompressedMsg = readBlob(entity, LogDataRecordKeys.logMessage);
      dataRecord.setLogMessage(deCompressString(byteCompressedMsg));
    } catch (Exception ex) {
      dataRecord.setLogMessage(null);
    }
    dataRecord.setUuid(entity.getKey().getName());
    dataRecord.setAppId(readString(entity, "appId"));

    return dataRecord;
  }
}
