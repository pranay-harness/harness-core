package io.harness.cdng.pipeline.executions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.pms.plan.execution.AccountExecutionInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CDAccountExecutionMetadataKeys")
@Entity(value = "cdAccountExecutionMetadata", noClassnameStored = true)
@Document("cdAccountExecutionMetadata")
@TypeAlias("cdAccountExecutionMetadata")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.NG_MANAGER)
public class CDAccountExecutionMetadata {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  Long executionCount;
  AccountExecutionInfo accountExecutionInfo;
}