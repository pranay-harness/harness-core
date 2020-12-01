package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/*
Stores histogram data for a single day
 */
@Data
@Builder
@FieldNameConstants(innerTypeName = "PartialRecommendationHistogramKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "partialRecommendationHistogram", noClassnameStored = true)
public class PartialRecommendationHistogram
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_clusterId_namespace_workloadName_workloadType_date")
                 .unique(true)
                 .field(PartialRecommendationHistogramKeys.accountId)
                 .field(PartialRecommendationHistogramKeys.clusterId)
                 .field(PartialRecommendationHistogramKeys.namespace)
                 .field(PartialRecommendationHistogramKeys.workloadName)
                 .field(PartialRecommendationHistogramKeys.workloadType)
                 .field(PartialRecommendationHistogramKeys.date)
                 .build())
        .build();
  }

  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  @NotEmpty String namespace;
  @NotEmpty String workloadName;
  @NotEmpty String workloadType;
  // Date for which the data corresponds to.
  Instant date;

  @NotEmpty Map<String, ContainerCheckpoint> containerCheckpoints;
}
