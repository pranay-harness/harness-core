package software.wings.service.impl.ldap;

import org.ldaptive.LdapException;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;

import java.util.function.Function;

/**
 * Function to fetch ldap group search results
 */
public class ExecuteLdapGroupsSearchRequest implements Function<LdapListGroupsRequest, LdapListGroupsResponse> {
  private static final Logger logger = LoggerFactory.getLogger(ExecuteLdapGroupsSearchRequest.class);

  @Override
  public LdapListGroupsResponse apply(LdapListGroupsRequest ldapListGroupsRequest) {
    Status searchStatus = Status.FAILURE;
    String searchStatusMessage = null;

    LdapSearch ldapSearch = ldapListGroupsRequest.getLdapSearch();
    LdapGroupConfig ldapGroupConfig = ldapListGroupsRequest.getLdapGroupConfig();

    SearchResult searchResult = null;
    try {
      searchResult = ldapSearch.execute(ldapListGroupsRequest.getReturnArguments());
    } catch (LdapException le) {
      logger.error("LdapException occurred while searchGroupbyName for base {} and searchQuery {}",
          ldapSearch.getBaseDN(), ldapSearch.getSearchFilter());
      searchStatusMessage = le.getResultCode().toString();
    }

    if (searchResult != null) {
      searchStatus = Status.SUCCESS;
    }

    LdapResponse ldapResponse = LdapResponse.builder().status(searchStatus).message(searchStatusMessage).build();
    return new LdapListGroupsResponse(searchResult, ldapResponse, ldapGroupConfig);
  }
}
