/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AuditHeaderYamlResponse {
  private String auditHeaderId;
  private String entityId;
  private String oldYaml;
  private String oldYamlPath;
  private String newYaml;
  private String newYamlPath;
}
