package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * Contract for any ldap config which will be used for searching
 *
 * @author Swapnil
 */
@OwnedBy(HarnessTeam.PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public interface LdapSearchConfig {
  String getBaseDN();

  void setBaseDN(String dn);

  String[] getReturnAttrs();
}
