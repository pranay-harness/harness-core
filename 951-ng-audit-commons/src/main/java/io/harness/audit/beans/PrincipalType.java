package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum PrincipalType { USER, SYSTEM, API_KEY, SERVICE_ACCOUNT }
