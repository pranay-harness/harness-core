package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "ProjectFilter", description = "This is Project filter entity as defined in harness")
public class ProjectFilterDTO {
  String searchTerm;
  Set<String> orgIdentifiers;
  Boolean hasModule;
  ModuleType moduleType;
  List<String> identifiers;
}
