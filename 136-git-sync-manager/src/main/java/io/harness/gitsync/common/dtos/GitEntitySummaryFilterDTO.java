package io.harness.gitsync.common.dtos;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.DX)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitEntityFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
public class GitEntitySummaryFilterDTO {
  ModuleType moduleType;
  List<String> gitSyncConfigIdentifiers;
  List<EntityType> entityTypes;
  String searchTerm;
}
