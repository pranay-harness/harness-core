package software.wings.beans;

import static software.wings.common.Constants.ECS_BG_TYPE_DNS;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowCreationFlags {
  private String ecsBGType;

  public boolean isEcsBgDnsType() {
    return ECS_BG_TYPE_DNS.equals(ecsBGType);
  }
}