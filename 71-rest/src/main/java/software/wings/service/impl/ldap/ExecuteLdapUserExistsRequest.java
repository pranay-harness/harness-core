package software.wings.service.impl.ldap;

import org.ldaptive.LdapException;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import java.util.function.Function;

public class ExecuteLdapUserExistsRequest implements Function<LdapUserExistsRequest, LdapUserExistsResponse> {
  private static final Logger logger = LoggerFactory.getLogger(ExecuteLdapUserExistsRequest.class);

  @Override
  public LdapUserExistsResponse apply(LdapUserExistsRequest ldapUserExistsRequest) {
    Status searchStatus;
    String searchStatusMsg = null;

    LdapSearch ldapSearch = ldapUserExistsRequest.getLdapSearch();
    String identifier = ldapUserExistsRequest.getIdentifier();
    LdapUserConfig ldapUserConfig = ldapUserExistsRequest.getLdapUserConfig();

    SearchDnResolver resolver = ldapSearch.getSearchDnResolver(ldapUserConfig.getUserFilter());
    String dn = null;
    try {
      dn = resolver.resolve(new User(identifier));
      searchStatus = Status.SUCCESS;
    } catch (LdapException le) {
      logger.error(
          "LdapException exception occured when executing user exits request for baseDN = {}, searchFilter = {} ",
          ldapUserConfig.getSearchFilter(), ldapUserConfig.getSearchFilter());
      searchStatus = Status.FAILURE;
      searchStatusMsg = le.getResultCode().toString();
    }

    LdapResponse ldapResponse = LdapResponse.builder().status(searchStatus).message(searchStatusMsg).build();

    return new LdapUserExistsResponse(ldapUserConfig, ldapResponse, dn);
  }
}
