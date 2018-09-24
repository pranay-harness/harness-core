package software.wings.beans.sso;

/**
 * Contract for any ldap config which will be used for searching
 *
 * @author Swapnil
 */
public interface LdapSearchConfig {
  String getBaseDN();

  void setBaseDN(String dn);

  String[] getReturnAttrs();
}
