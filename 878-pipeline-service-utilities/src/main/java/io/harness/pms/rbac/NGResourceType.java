package io.harness.pms.rbac;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class NGResourceType {
  public String SERVICE = "SERVICE";
  public String PIPELINE = "PIPELINE";
  public String ENVIRONMENT = "ENVIRONMENT";
  public String CONNECTOR = "CONNECTOR";
  public String SECRETS = "SECRET";
}
