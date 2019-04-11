package io.harness.k8s.model;

import io.harness.context.GlobalContextData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditGlobalContextData implements GlobalContextData {
  public static final String AUDIT_ID = "AUDIT_ID";
  private String auditId;

  @Override
  public String getKey() {
    return AUDIT_ID;
  }
}
