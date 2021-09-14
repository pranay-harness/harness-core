/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.globalcontex;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("AuditGlobalContextData")
public class AuditGlobalContextData implements GlobalContextData {
  public static final String AUDIT_ID = "AUDIT_ID";
  private String auditId;
  @Builder.Default private Set<EntityOperationIdentifier> entityOperationIdentifierSet = new HashSet<>();

  @Override
  public String getKey() {
    return AUDIT_ID;
  }
}
