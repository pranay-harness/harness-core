package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Getter
@Builder
public class AuditEntry {
  @NotNull @NotBlank String insertId;
  @NotNull @Valid ResourceDTO resource;
  @NotNull @Valid ResourceScopeDTO resourceScope;
  @NotNull Action action;
  @NotNull ModuleType module;
  AuditEventData auditEventData;
  Environment environment;
  String oldYaml;
  String newYaml;
  long timestamp;
}
