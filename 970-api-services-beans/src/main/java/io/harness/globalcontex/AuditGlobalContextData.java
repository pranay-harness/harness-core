/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
