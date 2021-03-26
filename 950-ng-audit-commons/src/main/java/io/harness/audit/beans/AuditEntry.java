package io.harness.audit.beans;

import io.harness.ModuleType;
import io.harness.ng.core.Resource;
import io.harness.scope.ResourceScope;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditEntry {
  @NotNull String insertId;
  Resource resource;
  ResourceScope resourceScope;
  String action;
  ModuleType module;
  String oldYaml;
  String newYaml;
  long timestamp;
}
