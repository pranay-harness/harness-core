package io.harness.accesscontrol.permissions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum PermissionStatus { STAGING, EXPERIMENTAL, ACTIVE, DEPRECATED, INACTIVE }
