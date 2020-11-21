package io.harness.ng.core.dto;

import io.harness.ModuleType;

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
  Boolean hasModule;
  ModuleType moduleType;
}
