package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "OrganizationFilter", description = "This has the details of Organization filter as defined in harness")
public class OrganizationFilterDTO {
  @Schema(description = NGResourceFilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = NGResourceFilterConstants.IDENTIFIER_LIST) List<String> identifiers;
  @Schema(description = NGResourceFilterConstants.IGNORE_CASE) boolean ignoreCase;
}
