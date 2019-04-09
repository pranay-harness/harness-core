package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readBlob;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
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
@Entity(value = "logDataRecords", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("stateType")
        , @Field("stateExecutionId"), @Field("host"), @Field("timeStamp"), @Field("logMD5Hash"), @Field("clusterLevel"),
            @Field("clusterLevel"), @Field("logCollectionMinute")
      },
      options = @IndexOptions(unique = true, name = "logUniqueIdx"))
  ,
      @Index(fields = {
        @Field("cvConfigId"), @Field("logCollectionMinute")
      }, options = @IndexOptions(unique = false, name = "cvLogsIdx"))
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "logMessage"})
public class LogDataRecord extends Base implements GoogleDataStoreAware {
  @NotEmpty private StateType stateType;

  @NotEmpty private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty private String serviceId;

  @NotEmpty @Indexed private String stateExecutionId;

  @Indexed private String cvConfigId;

  @NotEmpty private String query;

  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty private long timeStamp;

  private int timesLabeled;
  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @NotEmpty private ClusterLevel clusterLevel;
  @NotEmpty private long logCollectionMinute;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  public static List<LogDataRecord> generateDataRecords(StateType stateType, String applicationId, String cvConfigId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, ClusterLevel heartbeat, List<LogElement> logElements) {
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
    addFieldIfNotEmpty(dataStoreRecordBuilder, "stateType", stateType.name(), true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "appId", appId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "workflowId", workflowId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "workflowExecutionId", workflowExecutionId, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "serviceId", serviceId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "cvConfigId", cvConfigId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, "stateExecutionId", stateExecutionId, true);
    dataStoreRecordBuilder.set("timeStamp", timeStamp);
    dataStoreRecordBuilder.set("logCollectionMinute", logCollectionMinute);
    dataStoreRecordBuilder.set("timesLabeled", timesLabeled);
    dataStoreRecordBuilder.set("query", query);
    dataStoreRecordBuilder.set("clusterLabel", clusterLabel);
    dataStoreRecordBuilder.set("logMD5Hash", logMD5Hash);

    dataStoreRecordBuilder.set("clusterLevel", String.valueOf(clusterLevel));
    dataStoreRecordBuilder.set("count", String.valueOf(count));
    try {
      Blob compressedLog = Blob.copyFrom(compressString(logMessage));
      dataStoreRecordBuilder.set("logMessage", compressedLog);
    } catch (Exception ex) {
      return null;
    }
    addFieldIfNotEmpty(dataStoreRecordBuilder, "host", host, true);

    if (validUntil == null) {
      validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
    }
    dataStoreRecordBuilder.set("validUntil", validUntil.getTime());

    return dataStoreRecordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final LogDataRecord dataRecord = LogDataRecord.builder()
                                         .stateType(StateType.valueOf(readString(entity, "stateType")))
                                         .serviceId(readString(entity, "serviceId"))
                                         .cvConfigId(readString(entity, "cvConfigId"))
                                         .query(readString(entity, "query"))
                                         .clusterLabel(readString(entity, "clusterLabel"))
                                         .host(readString(entity, "host"))
                                         .timeStamp(Long.parseLong(readString(entity, "timeStamp")))
                                         .timesLabeled(Integer.parseInt(readString(entity, "timesLabeled")))
                                         .count(Integer.parseInt(readString(entity, "count")))
                                         .logMD5Hash(readString(entity, "logMD5Hash"))
                                         .clusterLevel(ClusterLevel.valueOf(readString(entity, "clusterLevel")))
                                         .workflowId(readString(entity, "workflowId"))
                                         .workflowExecutionId(readString(entity, "workflowExecutionId"))
                                         .stateExecutionId(readString(entity, "stateExecutionId"))
                                         .build();

    try {
      byte[] byteCompressedMsg = readBlob(entity, "logMessage");
      dataRecord.setLogMessage(deCompressString(byteCompressedMsg));
    } catch (Exception ex) {
      dataRecord.setLogMessage(null);
    }
    dataRecord.setUuid(entity.getKey().getName());
    dataRecord.setAppId(readString(entity, "appId"));

    return dataRecord;
  }
}
