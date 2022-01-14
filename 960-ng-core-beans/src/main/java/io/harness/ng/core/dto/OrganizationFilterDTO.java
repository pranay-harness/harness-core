package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.OrganizationConstants;
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
@Schema(name = "OrganizationFilter",
    description =
        "This has the Organization filter details. This is used to filter Organizations according to the details specified.")
public class OrganizationFilterDTO {
  @Schema(description = OrganizationConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = OrganizationConstants.IDENTIFIER_LIST) List<String> identifiers;
  @Schema(description = OrganizationConstants.IGNORE_CASE) boolean ignoreCase;
}
