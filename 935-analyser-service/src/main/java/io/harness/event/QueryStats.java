package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.alerts.AlertMetadata;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "QueryStatsKeys")
@Entity(value = "queryStats", noClassnameStored = true)
@Document("queryStats")
@TypeAlias("queryStats")
@HarnessEntity(exportable = true)
public class QueryStats {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("hash_serviceName_version_idx")
                 .unique(true)
                 .field(QueryStatsKeys.hash)
                 .field(QueryStatsKeys.serviceName)
                 .field(QueryStatsKeys.version)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("alertList_category_idx")
                 .field(QueryStatsKeys.alerts + ".alertCategory")
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;
  @NonNull @Getter String hash;
  @NonNull String version;
  @NonNull String serviceName;

  QueryExplainResult explainResult;
  String data;
  Boolean indexUsed;
  List<AlertMetadata> alerts;
  ParsedQuery parsedQuery;
  String collectionName;

  @Getter Long count;
  @CreatedDate Long createdAt;

  long executionTimeMillis;
}
