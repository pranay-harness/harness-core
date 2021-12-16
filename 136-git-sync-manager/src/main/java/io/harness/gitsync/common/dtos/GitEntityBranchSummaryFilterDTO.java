package io.harness.gitsync.common.dtos;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
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
@ApiModel("GitEntityBranchFilterSummaryProperties")
@Schema(name = "GitEntityBranchSummaryFilter",
    description = "This contains filters for Git Sync Entity such as Module Type, Entity Type and Search Term")
@NoArgsConstructor
@AllArgsConstructor
public class GitEntityBranchSummaryFilterDTO {
  @Schema(description = "Module Type") ModuleType moduleType;
  @Schema(description = "List of Entity Types") List<EntityType> entityTypes;
  @Schema(description = GitSyncApiConstants.SEARCH_TERM_PARAM_MESSAGE) String searchTerm;
}
