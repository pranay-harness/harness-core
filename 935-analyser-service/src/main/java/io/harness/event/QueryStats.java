package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
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
  @NonNull @Getter String hash;
  @NonNull String version;
  @NonNull String serviceName;

  QueryExplainResult explainResult;
  String data;
  Boolean indexUsed;
  QueryAlertCategory alertCategory;
  @Getter Long count;
}
