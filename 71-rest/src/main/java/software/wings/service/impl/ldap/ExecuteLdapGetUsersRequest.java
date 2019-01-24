package software.wings.service.impl.ldap;

import org.ldaptive.LdapException;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import java.util.function.Function;

public class ExecuteLdapGetUsersRequest implements Function<LdapGetUsersRequest, LdapGetUsersResponse> {
  private static final Logger logger = LoggerFactory.getLogger(ExecuteLdapGetUsersRequest.class);

  @Override
  public LdapGetUsersResponse apply(LdapGetUsersRequest ldapGetUsersRequest) {
    Status searchStatus = Status.FAILURE;
    String searchStatusMsg = null;

    LdapSearch ldapSearch = ldapGetUsersRequest.getLdapSearch();
    LdapUserConfig ldapUserConfig = ldapGetUsersRequest.getLdapUserConfig();
    SearchResult searchResult = null;
    try {
      searchResult = ldapSearch.execute(ldapUserConfig.getReturnAttrs());
      if (searchResult == null || searchResult.size() == 0) {
        logger.info("Got zero results with regular search, trying with fallbackLDAPSearch instead");
        searchResult = getFallBackLdapSearch(ldapSearch).execute(ldapUserConfig.getReturnAttrs());
      }
    } catch (LdapException le) {
      if (ResultCode.UNAVAILABLE_CRITICAL_EXTENSION == le.getResultCode()) {
        try {
          searchResult = getFallBackLdapSearch(ldapSearch).execute(ldapUserConfig.getReturnAttrs());
        } catch (LdapException ldapException) {
          logger.error("LdapException exception occurred for user config with baseDN = {}, searchFilter = {} ",
              ldapUserConfig.getSearchFilter(), ldapUserConfig.getSearchFilter());
          searchStatusMsg = ldapException.getResultCode().toString();
        }
      } else {
        logger.error("LdapException exception occurred for user config with baseDN = {}, searchFilter = {} ",
            ldapUserConfig.getSearchFilter(), ldapUserConfig.getSearchFilter());
        searchStatusMsg = le.getResultCode().toString();
      }
    }

    if (searchResult != null) { // Scenario when search result
      searchStatus = Status.SUCCESS;
    }

    LdapResponse ldapResponse = LdapResponse.builder().status(searchStatus).message(searchStatusMsg).build();

    return new LdapGetUsersResponse(ldapUserConfig, ldapResponse, searchResult);
  }

  private LdapSearch getFallBackLdapSearch(LdapSearch ldapSearch) {
    return LdapSearch.builder()
        .baseDN(ldapSearch.getBaseDN())
        .connectionFactory(ldapSearch.getConnectionFactory())
        .referralsEnabled(ldapSearch.isReferralsEnabled())
        .maxReferralHops(ldapSearch.getMaxReferralHops())
        .searchFilter(ldapSearch.getFallBackSearchFilter())
        .build();
  }
}
