package io.harness.gitsync.fullsync.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "FullSyncEntityInfo", description = "Contains full sync information for entity")
@OwnedBy(PL)
public class GitFullSyncEntityInfoDTO {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String filePath;
  EntityType entityType;
  GitFullSyncEntityInfo.SyncStatus syncStatus;
  String name;
  String branch;
  String repo;
  long retryCount;
  List<String> errorMessages;
}
