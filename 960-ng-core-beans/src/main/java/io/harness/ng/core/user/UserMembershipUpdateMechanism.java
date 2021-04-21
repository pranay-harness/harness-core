package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum UserMembershipUpdateMechanism { ACCEPTED_INVITE, LDAP, AUTHORIZED_USER, SYSTEM }
