package software.wings.helpers.ext.ldap;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public interface LdapConnectionConfig {
  int getConnectTimeout();

  int getResponseTimeout();

  boolean isSslEnabled();

  boolean isReferralsEnabled();

  int getMaxReferralHops();

  String getBindDN();

  String getBindPassword();

  String generateUrl();
}
