package io.harness.ng.accesscontrol.migrations.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessControlMigrationPrincipal {
  String identifier;
  String email;
  String name;
}
