package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.common.hash.Hashing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Entity(value = "labeledLogRecords", noClassnameStored = true)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class LabeledLogRecord implements GoogleDataStoreAware {
  private String logMessage;
  private List<LogLabel> labels;
  private int timesLabeled;
  private long createdAt;
  @Id String dataRecordId;

  private transient LogDataRecord dataRecord;

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(dataRecordId);
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, "logMessage", logMessage, true);
    addFieldIfNotEmpty(recordBuilder, "dataRecordId", dataRecordId, false);
    addFieldIfNotEmpty(recordBuilder, "timesLabeled", String.valueOf(timesLabeled), true);
    if (isNotEmpty(labels)) {
      addFieldIfNotEmpty(recordBuilder, "labels", JsonUtils.asJson(labels), true);
    }
    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final LabeledLogRecord dataRecord = LabeledLogRecord.builder()
                                            .dataRecordId(readString(entity, "dataRecordId"))
                                            .logMessage(readString(entity, "logMessage"))
                                            .timesLabeled(Integer.parseInt(readString(entity, "timesLabeled")))
                                            .build();

    final String labelsJson = readString(entity, "labels");
    if (isNotEmpty(labelsJson)) {
      dataRecord.setLabels(JsonUtils.asObject(labelsJson, new TypeReference<List<LogLabel>>() {}));
    }

    return dataRecord;
  }

  private String generateUniqueKey() {
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(logMessage);
    appendIfNecessary(keyBuilder, dataRecordId);
    return Hashing.sha256().hashString(keyBuilder.toString(), StandardCharsets.UTF_8).toString();
  }
  private void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (isNotEmpty(value)) {
      keyBuilder.append(':').append(value);
    }
  }
}
