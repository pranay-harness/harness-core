package software.wings.helpers.ext.ldap;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SearchScope;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.handler.RecursiveEntryHandler;
import org.ldaptive.referral.SearchReferralHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

// TODO: Move these to  portal/rest/src/main/java/software/wings/security
@Builder(builderClassName = "Builder", buildMethodName = "internalBuild")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapSearch implements LdapValidator {
  @NotNull ConnectionFactory connectionFactory;
  @NotNull String baseDN;
  @NotBlank String searchFilter;
  @NotNull @Default SearchScope searchScope = SearchScope.SUBTREE;
  int limit;
  boolean referralsEnabled;
  int maxReferralHops;
  String recursiveSearchAttr;
  String recursiveMergeAttr;

  private static Logger logger = LoggerFactory.getLogger(LdapSearch.class);

  public static class Builder {
    public LdapSearch build() {
      LdapSearch search = internalBuild();
      search.validate(logger);
      return search;
    }
  }

  public SearchResult execute(String... attrs) throws LdapException {
    SearchRequest request = new SearchRequest();

    request.setBaseDn(baseDN);
    request.setSearchFilter(new SearchFilter(searchFilter));
    request.setSizeLimit(limit);
    request.setSearchScope(searchScope);

    if (StringUtils.isNotBlank(recursiveSearchAttr) && StringUtils.isNotBlank(recursiveMergeAttr)) {
      request.setSearchEntryHandlers(new RecursiveEntryHandler(recursiveSearchAttr, recursiveMergeAttr));
    }

    if (ArrayUtils.isNotEmpty(attrs)) {
      request.setReturnAttributes(attrs);
    }

    if (referralsEnabled) {
      request.setReferralHandler(new SearchReferralHandler(maxReferralHops));
    }

    try (Connection connection = connectionFactory.getConnection()) {
      connection.open();
      SearchOperation search = new SearchOperation(connection);
      return search.execute(request).getResult();
    }
  }

  public SearchDnResolver getSearchDnResolver(String userFilter) {
    SearchDnResolver resolver = new SearchDnResolver(connectionFactory);
    resolver.setBaseDn(baseDN);
    resolver.setUserFilter(userFilter);
    if (searchScope.equals(SearchScope.SUBTREE)) {
      resolver.setSubtreeSearch(true);
    }
    return resolver;
  }
}
