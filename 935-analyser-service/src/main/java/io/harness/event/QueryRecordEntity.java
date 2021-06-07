package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "QueryRecordEntityKeys")
@Entity(value = "queryRecords", noClassnameStored = true)
@Document("queryRecords")
@TypeAlias("queryRecords")
@HarnessEntity(exportable = true)
public class QueryRecordEntity {
  String hash;
  QueryExplainResult explainResult;
  String data;
  String version;
  String serviceName;
}
