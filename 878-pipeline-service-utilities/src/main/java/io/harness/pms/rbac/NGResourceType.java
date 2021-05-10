package io.harness.pms.rbac;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class NGResourceType {
  public final String SERVICE = "SERVICE";
  public final String PIPELINE = "PIPELINE";
  public final String ENVIRONMENT = "ENVIRONMENT";
  public final String CONNECTOR = "CONNECTOR";
  public final String SECRETS = "SECRET";
}
