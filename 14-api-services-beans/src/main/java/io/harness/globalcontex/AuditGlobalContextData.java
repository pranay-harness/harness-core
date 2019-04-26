package io.harness.globalcontex;

import io.harness.context.GlobalContextData;
import lombok.Builder;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
@Builder
public class AuditGlobalContextData implements GlobalContextData {
  public static final String AUDIT_ID = "AUDIT_ID";
  private String auditId;
  @Builder.Default private Set<EntityOperationIdentifier> entityOperationIdentifierSet = new HashSet<>();

  @Override
  public String getKey() {
    return AUDIT_ID;
  }
}
