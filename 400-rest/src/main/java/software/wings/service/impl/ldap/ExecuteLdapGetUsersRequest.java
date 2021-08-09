package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.LdapException;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchResult;

@OwnedBy(PL)
@Slf4j
public class ExecuteLdapGetUsersRequest implements Function<LdapGetUsersRequest, LdapGetUsersResponse> {
  @Override
  public LdapGetUsersResponse apply(LdapGetUsersRequest ldapGetUsersRequest) {
    Status searchStatus = Status.FAILURE;
    String searchStatusMsg = null;
    ResultCode ldapResultCode = null;

    LdapSearch ldapSearch = ldapGetUsersRequest.getLdapSearch();
    LdapUserConfig ldapUserConfig = ldapGetUsersRequest.getLdapUserConfig();
    SearchResult searchResult = null;
    try {
      searchResult = ldapSearch.execute(ldapUserConfig.getReturnAttrs());
      if (searchResult == null || searchResult.size() == 0) {
        log.info("Got zero results with regular search, trying with fallbackLDAPSearch instead for search to work");
        searchResult = getFallBackLdapSearch(ldapSearch).execute(ldapUserConfig.getReturnAttrs());
      }
    } catch (LdapException le) {
      ldapResultCode = le.getResultCode();
      try {
        log.info("LDAP Search failed errorCode = {} , trying fallback ldapSearch", ldapResultCode);
        searchResult = getFallBackLdapSearch(ldapSearch).execute(ldapUserConfig.getReturnAttrs());
      } catch (LdapException ldapException) {
        ldapResultCode = ldapException.getResultCode();
        log.error("LdapException ErrorCode = {}", ldapResultCode);
        log.error("LdapException exception occurred for user config with baseDN = {}, searchFilter = {}",
            ldapUserConfig.getBaseDN(), ldapUserConfig.getSearchFilter());
      }
      searchStatusMsg = ldapResultCode.toString();
    }

    if (searchResult != null) { // Scenario when search result
      searchStatus = Status.SUCCESS;
    }

    LdapResponse ldapResponse = LdapResponse.builder().status(searchStatus).message(searchStatusMsg).build();

    return new LdapGetUsersResponse(ldapUserConfig, ldapResponse, searchResult, ldapGetUsersRequest.getGroupBaseDn());
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
