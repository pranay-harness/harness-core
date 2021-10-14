package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AccountExecutionMetadataKeys")
@Entity(value = "accountExecutionMetadata", noClassnameStored = true)
@Document("accountExecutionMetadata")
@TypeAlias("accountExecutionMetadata")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.PMS)
public class AccountExecutionMetadata {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  Map<String, Long> moduleToExecutionCount;
  Map<String, AccountExecutionInfo> moduleToExecutionInfoMap;
}