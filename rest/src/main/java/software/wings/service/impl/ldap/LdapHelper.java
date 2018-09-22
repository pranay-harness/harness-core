package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.ConnectionInitializer;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchResult;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.BindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.User;
import software.wings.beans.sso.LdapSearchConfig;
import software.wings.helpers.ext.ldap.LdapConnectionConfig;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import java.time.Duration;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapHelper {
  LdapConnectionConfig connectionConfig;
  ConnectionConfig ldaptiveConfig;

  public LdapHelper(LdapConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
    this.ldaptiveConfig = buildLdaptiveConfig(connectionConfig);
  }

  private static ConnectionInitializer getConnectionInitializer(LdapConnectionConfig connectionConfig) {
    return new BindConnectionInitializer(
        connectionConfig.getBindDN(), new Credential(connectionConfig.getBindPassword()));
  }

  private static ConnectionConfig buildLdaptiveConfig(LdapConnectionConfig connectionConfig) {
    ConnectionConfig config = new ConnectionConfig(connectionConfig.generateUrl());
    config.setConnectTimeout(Duration.ofMillis(connectionConfig.getConnectTimeout()));
    config.setResponseTimeout(Duration.ofMillis(connectionConfig.getResponseTimeout()));
    config.setConnectionInitializer(getConnectionInitializer(connectionConfig));
    config.setUseSSL(connectionConfig.isSslEnabled());
    return config;
  }

  public ConnectionFactory getConnectionFactory() {
    return new DefaultConnectionFactory(ldaptiveConfig);
  }

  private Connection getConnection() throws LdapException {
    return getConnectionFactory().getConnection();
  }

  public LdapResponse validateConnectionConfig() {
    try (Connection connection = getConnection()) {
      connection.open();
      return LdapResponse.builder().status(Status.SUCCESS).message(LdapConstants.CONNECTION_SUCCESS).build();
    } catch (LdapException e) {
      return LdapResponse.builder().status(Status.FAILURE).message(e.getResultCode().toString()).build();
    }
  }

  public LdapResponse validateUserConfig(LdapUserConfig config) {
    try {
      LdapResponse connectionTestResponse = validateConnectionConfig();
      if (connectionTestResponse.getStatus().equals(Status.FAILURE)) {
        return connectionTestResponse;
      }

      LdapSearch search = LdapSearch.builder()
                              .connectionFactory(getConnectionFactory())
                              .baseDN(config.getBaseDN())
                              .searchFilter(config.getLoadUsersFilter())
                              .limit(LdapConstants.MIN_USER_QUERY_SIZE)
                              .build();

      if (search.execute().size() == 0) {
        return LdapResponse.builder().status(Status.FAILURE).message(LdapConstants.USER_CONFIG_FAILURE).build();
      }

      return LdapResponse.builder().status(Status.SUCCESS).message(LdapConstants.USER_CONFIG_SUCCESS).build();
    } catch (LdapException e) {
      return LdapResponse.builder().status(Status.FAILURE).message(e.getResultCode().toString()).build();
    }
  }

  public LdapResponse validateGroupConfig(LdapGroupConfig config) {
    try {
      LdapResponse connectionTestResponse = validateConnectionConfig();
      if (connectionTestResponse.getStatus().equals(Status.FAILURE)) {
        return connectionTestResponse;
      }
      if (0 == listGroups(config, null, LdapConstants.MIN_GROUP_QUERY_SIZE).size()) {
        return LdapResponse.builder().status(Status.FAILURE).message(LdapConstants.GROUP_CONFIG_FAILURE).build();
      }
      return LdapResponse.builder().status(Status.SUCCESS).message(LdapConstants.GROUP_CONFIG_SUCCESS).build();
    } catch (LdapException e) {
      return LdapResponse.builder().status(Status.FAILURE).message(e.getResultCode().toString()).build();
    }
  }

  public LdapSearch.Builder getDefaultLdapSearchBuilder(LdapSearchConfig config) {
    return LdapSearch.builder()
        .baseDN(config.getBaseDN())
        .connectionFactory(getConnectionFactory())
        .referralsEnabled(connectionConfig.isReferralsEnabled())
        .maxReferralHops(connectionConfig.getMaxReferralHops());
  }

  public boolean userExists(LdapUserConfig config, String identifier) throws LdapException {
    LdapSearch search = getDefaultLdapSearchBuilder(config).searchFilter(config.getSearchFilter()).build();
    SearchDnResolver resolver = search.getSearchDnResolver(config.getUserFilter());
    String Dn = resolver.resolve(new User(identifier));
    return StringUtils.isNotBlank(Dn);
  }

  public SearchResult listGroups(LdapGroupConfig config, String additionalFilter, int limit, String... returnFields)
      throws LdapException {
    LdapSearch search =
        getDefaultLdapSearchBuilder(config).limit(limit).searchFilter(config.getFilter(additionalFilter)).build();
    return search.execute(returnFields);
  }

  public SearchResult listGroupsByName(LdapGroupConfig groupConfig, String name) throws LdapException {
    return listGroups(groupConfig, String.format("*%s*", name), LdapConstants.MAX_GROUP_SEARCH_SIZE);
  }

  public SearchResult listGroupUsers(LdapUserConfig userConfig, String groupDn) throws LdapException {
    LdapSearch search =
        getDefaultLdapSearchBuilder(userConfig).searchFilter(userConfig.getGroupMembershipFilter(groupDn)).build();
    return search.execute(userConfig.getReturnAttrs());
  }

  public int getGroupUserCount(LdapUserConfig config, String groupDn) throws LdapException {
    return listGroupUsers(config, groupDn).size();
  }

  public void populateGroupSize(SearchResult groups, LdapUserConfig config) throws LdapException {
    for (LdapEntry group : groups.getEntries()) {
      int groupSize = getGroupUserCount(config, group.getDn());
      LdapAttribute groupSizeAttr = new LdapAttribute("groupSize", String.valueOf(groupSize));
      group.addAttribute(groupSizeAttr);
    }
  }

  public SearchResult searchGroupsByName(LdapGroupConfig groupConfig, String name) throws LdapException {
    return listGroupsByName(groupConfig, name);
  }

  public SearchResult getGroupByDn(LdapGroupConfig groupConfig, String dn) throws LdapException {
    String oldBaseDn = groupConfig.getBaseDN();
    groupConfig.setBaseDN(dn);
    SearchResult groups = listGroups(groupConfig, null, 1, groupConfig.getReturnAttrs());
    groupConfig.setBaseDN(oldBaseDn);
    return groups;
  }

  public LdapResponse authenticate(LdapUserConfig config, String identifier, String password) {
    try {
      if (!userExists(config, identifier)) {
        return LdapResponse.builder().status(Status.FAILURE).message(LdapConstants.USER_NOT_FOUND).build();
      }

      LdapSearch search = LdapSearch.builder()
                              .baseDN(config.getBaseDN())
                              .connectionFactory(getConnectionFactory())
                              .searchFilter(config.getSearchFilter())
                              .referralsEnabled(connectionConfig.isReferralsEnabled())
                              .maxReferralHops(connectionConfig.getMaxReferralHops())
                              .build();

      SearchDnResolver resolver = search.getSearchDnResolver(config.getUserFilter());

      BindAuthenticationHandler handler = new BindAuthenticationHandler(getConnectionFactory());

      Authenticator authenticator = new Authenticator(resolver, handler);

      AuthenticationRequest request = new AuthenticationRequest(identifier, new Credential(password));
      AuthenticationResponse response = authenticator.authenticate(request);
      if (response.getResult()) {
        return LdapResponse.builder().status(Status.SUCCESS).message(LdapConstants.AUTHENTICATION_SUCCESS).build();
      } else {
        return LdapResponse.builder().status(Status.FAILURE).message(response.getResultCode().toString()).build();
      }
    } catch (LdapException e) {
      return LdapResponse.builder().status(Status.FAILURE).message(e.getResultCode().toString()).build();
    }
  }
}
