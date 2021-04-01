package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.ng.core.Resource;
import io.harness.scope.ResourceScope;

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
  @NotNull @Valid Resource resource;
  @NotNull @Valid ResourceScope resourceScope;
  @NotNull Action action;
  @NotNull ModuleType module;
  String environmentIdentifier;
  String oldYaml;
  String newYaml;
  long timestamp;
}
