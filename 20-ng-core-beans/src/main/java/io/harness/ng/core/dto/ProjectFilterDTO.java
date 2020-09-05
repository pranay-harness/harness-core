package io.harness.ng.core.dto;

import io.harness.ng.ModuleType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectFilterDTO {
  String searchTerm;
  String orgIdentifier;
  ModuleType moduleType;
}
