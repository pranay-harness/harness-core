package software.wings.helpers.ext.ldap;

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
