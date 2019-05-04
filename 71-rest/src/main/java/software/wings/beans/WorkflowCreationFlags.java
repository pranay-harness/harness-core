package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowCreationFlags {
  private static final String ECS_BG_TYPE_DNS = "DNS";
  private String ecsBGType;

  public boolean isEcsBgDnsType() {
    return ECS_BG_TYPE_DNS.equals(ecsBGType);
  }
}