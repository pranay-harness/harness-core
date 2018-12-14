package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readLong;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricRecords", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("name")
        , @Field("host"), @Field("timeStamp"), @Field("workflowExecutionId"), @Field("stateExecutionId"),
            @Field("serviceId"), @Field("workflowId"), @Field("level"), @Field("stateType"), @Field("groupName")
      },
      options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
  ,
      @Index(fields = {
        @Field("appId"), @Field("cvConfigId"), @Field("dataCollectionMinute")
      }, options = @IndexOptions(name = "timeSeriesIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetricDataRecord extends Base implements GoogleDataStoreAware {
  @Transient public static String DEFAULT_GROUP_NAME = "default";

  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String name;

  @Indexed private String workflowId;

  @Indexed private String workflowExecutionId;

  @Indexed private String serviceId;

  @Indexed private String cvConfigId;

  @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private long timeStamp;

  @Indexed private int dataCollectionMinute;

  @NotEmpty private String host;

  @Indexed private ClusterLevel level;

  private String tag;

  @Indexed private String groupName = DEFAULT_GROUP_NAME;

  private Map<String, Double> values = new HashMap<>();

  private Map<String, String> deeplinkMetadata = new HashMap<>();

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  @Builder
  public NewRelicMetricDataRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, StateType stateType,
      String name, String workflowId, String workflowExecutionId, String serviceId, String cvConfigId,
      String stateExecutionId, long timeStamp, int dataCollectionMinute, String host, ClusterLevel level, String tag,
      String groupName, Map<String, Double> values, Map<String, String> deeplinkMetadata) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateType = stateType;
    this.name = name;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.serviceId = serviceId;
    this.cvConfigId = cvConfigId;
    this.stateExecutionId = stateExecutionId;
    this.timeStamp = timeStamp;
    this.dataCollectionMinute = dataCollectionMinute;
    this.host = host;
    this.level = level;
    this.tag = tag;
    this.groupName = isEmpty(groupName) ? DEFAULT_GROUP_NAME : groupName;
    this.values = isEmpty(values) ? new HashMap<>() : values;
    this.deeplinkMetadata = isEmpty(deeplinkMetadata) ? new HashMap<>() : deeplinkMetadata;
    this.validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
  }

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(generateUniqueKey());
    com.google.cloud.datastore.Entity.Builder recordBuilder = com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(recordBuilder, "stateType", stateType.getName(), true);
    addFieldIfNotEmpty(recordBuilder, "appId", appId, false);
    addFieldIfNotEmpty(recordBuilder, "name", name, false);
    addFieldIfNotEmpty(recordBuilder, "workflowId", workflowId, true);
    addFieldIfNotEmpty(recordBuilder, "workflowExecutionId", workflowExecutionId, false);
    addFieldIfNotEmpty(recordBuilder, "serviceId", serviceId, false);
    addFieldIfNotEmpty(recordBuilder, "cvConfigId", cvConfigId, false);
    addFieldIfNotEmpty(recordBuilder, "stateExecutionId", stateExecutionId, false);
    recordBuilder.set("timeStamp", timeStamp);
    recordBuilder.set("dataCollectionMinute", dataCollectionMinute);
    addFieldIfNotEmpty(recordBuilder, "level", level == null ? null : level.name(), true);
    addFieldIfNotEmpty(recordBuilder, "tag", tag, true);
    addFieldIfNotEmpty(recordBuilder, "groupName", groupName, false);

    if (isNotEmpty(values)) {
      addFieldIfNotEmpty(recordBuilder, "values", JsonUtils.asJson(values), true);
    }
    if (isNotEmpty(deeplinkMetadata)) {
      addFieldIfNotEmpty(recordBuilder, "deeplinkMetadata", JsonUtils.asJson(deeplinkMetadata), true);
    }

    return recordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final NewRelicMetricDataRecord dataRecord =
        NewRelicMetricDataRecord.builder()
            .appId(readString(entity, "appId"))
            .stateType(StateType.valueOf(readString(entity, "stateType")))
            .name(readString(entity, "name"))
            .workflowId(readString(entity, "workflowId"))
            .workflowExecutionId(readString(entity, "workflowExecutionId"))
            .serviceId(readString(entity, "serviceId"))
            .cvConfigId(readString(entity, "cvConfigId"))
            .stateExecutionId(readString(entity, "stateExecutionId"))
            .timeStamp(readLong(entity, "timeStamp"))
            .dataCollectionMinute((int) readLong(entity, "dataCollectionMinute"))
            .host(readString(entity, "host"))
            .tag(readString(entity, "tag"))
            .groupName(readString(entity, "groupName"))
            .build();

    final String level = readString(entity, "level");
    if (isNotEmpty(level)) {
      dataRecord.setLevel(ClusterLevel.valueOf(level));
    }

    final String valuesJson = readString(entity, "values");
    if (isNotEmpty(valuesJson)) {
      dataRecord.setValues(JsonUtils.asObject(valuesJson, new TypeReference<Map<String, Double>>() {}));
    }

    final String deepLinkJson = readString(entity, "deeplinkMetadata");
    if (isNotEmpty(deepLinkJson)) {
      dataRecord.setDeeplinkMetadata(JsonUtils.asObject(deepLinkJson, new TypeReference<Map<String, String>>() {}));
    }
    return dataRecord;
  }

  private String generateUniqueKey() {
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(name);
    appendIfNecessary(keyBuilder, host);
    keyBuilder.append(':').append(timeStamp);
    appendIfNecessary(keyBuilder, workflowExecutionId);
    appendIfNecessary(keyBuilder, stateExecutionId);
    appendIfNecessary(keyBuilder, serviceId);
    appendIfNecessary(keyBuilder, workflowId);
    appendIfNecessary(keyBuilder, level == null ? null : level.name());
    appendIfNecessary(keyBuilder, stateType.name());
    appendIfNecessary(keyBuilder, groupName);
    return keyBuilder.toString();
  }

  private void appendIfNecessary(StringBuilder keyBuilder, String value) {
    if (isNotEmpty(value)) {
      keyBuilder.append(':').append(value);
    }
  }
}
