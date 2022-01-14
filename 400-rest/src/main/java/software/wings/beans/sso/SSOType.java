package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum SSOType { SAML, LDAP, OAUTH }
