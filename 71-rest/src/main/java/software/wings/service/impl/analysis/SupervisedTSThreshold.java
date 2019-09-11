package software.wings.service.impl.analysis;

import static software.wings.service.impl.GoogleDataStoreServiceImpl.addFieldIfNotEmpty;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readDouble;
import static software.wings.service.impl.GoogleDataStoreServiceImpl.readString;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;

import java.util.Arrays;
import java.util.List;

@org.mongodb.morphia.annotations.Entity(value = "supervisedTSThreshold", noClassnameStored = true)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "SupervisedTSThresholdKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SupervisedTSThreshold implements GoogleDataStoreAware, CreatedAtAware {
  public static final String connector = ":";

  @Id private String uuid;
  private String accountId;
  private String serviceId;
  private String transactionName;
  private String metricName;
  private MetricType metricType;
  private Double mean;
  private Double standardDeviation;
  private Double minThreshold;
  private Double maxThreshold;
  private Version version;
  private long createdAt;

  public static String getKey(
      String accountId, String serviceId, String transactionName, String metricName, Version version) {
    return String.join(connector, accountId, serviceId, transactionName, metricName, version.name());
  }

  @Override
  public Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey =
        datastore.newKeyFactory()
            .setKind(this.getClass().getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
            .newKey(this.getUuid() != null ? this.getUuid()
                                           : getKey(accountId, serviceId, transactionName, metricName, version));

    com.google.cloud.datastore.Entity.Builder dataStoreRecordBuilder =
        com.google.cloud.datastore.Entity.newBuilder(taskKey);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.accountId, accountId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.serviceId, serviceId, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.transactionName, transactionName, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.metricName, metricName, false);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.mean, mean, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.standardDeviation, standardDeviation, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.minThreshold, minThreshold, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.maxThreshold, maxThreshold, true);
    addFieldIfNotEmpty(dataStoreRecordBuilder, SupervisedTSThresholdKeys.version, version.name(), false);
    return dataStoreRecordBuilder.build();
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(Entity entity) {
    final SupervisedTSThreshold tsThreshold =
        SupervisedTSThreshold.builder()
            .accountId(readString(entity, SupervisedTSThresholdKeys.accountId))
            .serviceId(readString(entity, SupervisedTSThresholdKeys.serviceId))
            .transactionName(readString(entity, SupervisedTSThresholdKeys.transactionName))
            .metricName(readString(entity, SupervisedTSThresholdKeys.metricName))
            .mean(readDouble(entity, SupervisedTSThresholdKeys.mean))
            .standardDeviation(readDouble(entity, SupervisedTSThresholdKeys.mean))
            .minThreshold(readDouble(entity, SupervisedTSThresholdKeys.minThreshold))
            .maxThreshold(readDouble(entity, SupervisedTSThresholdKeys.maxThreshold))
            .version(Version.valueOf(readString(entity, SupervisedTSThresholdKeys.version)))
            .build();

    tsThreshold.setUuid(entity.getKey().getName());

    return tsThreshold;
  }

  public static List<Threshold> getThresholds(SupervisedTSThreshold threshold) {
    Threshold minThreshold = Threshold.builder()
                                 .thresholdType(ThresholdType.ALERT_WHEN_LOWER)
                                 .comparisonType(ThresholdComparisonType.DELTA)
                                 .ml(threshold.getMinThreshold())
                                 .build();
    Threshold maxThreshold = Threshold.builder()
                                 .thresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                 .comparisonType(ThresholdComparisonType.DELTA)
                                 .ml(threshold.getMaxThreshold())
                                 .build();
    return Arrays.asList(minThreshold, maxThreshold);
  }
}
