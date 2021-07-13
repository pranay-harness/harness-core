package io.harness.accesscontrol.roles;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
public class PrivilegedRolesConfig {
  String name;
  int version;
  Set<PrivilegedRole> roles;
}
