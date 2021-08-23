package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSearchConfig;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserSettings;
import software.wings.helpers.ext.ldap.LdapConnectionConfig;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import com.google.common.collect.Lists;
import de.danielbechler.util.Collections;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
import org.ldaptive.ssl.AllowAnyHostnameVerifier;
import org.ldaptive.ssl.AllowAnyTrustManager;
import org.ldaptive.ssl.SslConfig;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class LdapHelper {
  LdapConnectionConfig connectionConfig;
  ConnectionConfig ldaptiveConfig;
  LdapParallelSearchExecutor ldapParallelSearchExecutor;
  Function<LdapListGroupsRequest, LdapListGroupsResponse> executeLdapGroupsSearchRequest;
  Function<LdapUserExistsRequest, LdapUserExistsResponse> executeLdapUserExistsRequest;
  Function<LdapGetUsersRequest, LdapGetUsersResponse> executeLdapGetUsersRequest;

  public LdapHelper(LdapConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
    this.ldaptiveConfig = buildLdaptiveConfig(connectionConfig);
    // Somehow bindings were not working for these classes
    // Hence, I am manually instantiating them.  Will come back on this one
    ldapParallelSearchExecutor = new LdapParallelSearchExecutor();
    executeLdapGroupsSearchRequest = new ExecuteLdapGroupsSearchRequest();
    executeLdapUserExistsRequest = new ExecuteLdapUserExistsRequest();
    executeLdapGetUsersRequest = new ExecuteLdapGetUsersRequest();
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
    // Allowing self-signed certificates to be used for LDAP SSL connection.
    SslConfig sslConfig = new SslConfig(new AllowAnyTrustManager());
    sslConfig.setHostnameVerifier(new AllowAnyHostnameVerifier());
    config.setSslConfig(sslConfig);
    return config;
  }

  private ConnectionFactory getConnectionFactory() {
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
      log.error("Ldap connection validation failed for url: [{}]", connectionConfig.generateUrl(), e);
      return LdapResponse.builder().status(Status.FAILURE).message(e.getResultCode().toString()).build();
    }
  }

  LdapResponse validateUserConfig(List<? extends LdapUserConfig> configs) {
    Status searchStatus = Status.FAILURE;
    String searchStatusMsg = null;

    LdapResponse connectionTestResponse = validateConnectionConfig();
    if (connectionTestResponse.getStatus() == Status.FAILURE) {
      searchStatus = connectionTestResponse.getStatus();
      searchStatusMsg = connectionTestResponse.getMessage();
    } else { // now test for user config
      if (!Collections.isEmpty(configs)) {
        LdapUserConfig config = configs.get(0);
        SearchResult searchResult = null;
        try {
          LdapSearch search = LdapSearch.builder()
                                  .connectionFactory(getConnectionFactory())
                                  .baseDN(config.getBaseDN())
                                  .searchFilter(config.getLoadUsersFilter())
                                  .limit(LdapConstants.MIN_USER_QUERY_SIZE)
                                  .build();

          searchResult = search.execute();

        } catch (LdapException e) {
          log.error("LdapException occurred when validating user config with baseDN = {} and search filter = {}",
              config.getBaseDN(), config.getSearchFilter());
          searchStatusMsg = e.getResultCode().toString();
        }

        if (searchResult != null) { // Scenario when search result
          if (searchResult.size() > 0) {
            searchStatus = Status.SUCCESS;
            searchStatusMsg = LdapConstants.USER_CONFIG_SUCCESS;
          } else {
            searchStatusMsg = LdapConstants.USER_CONFIG_FAILURE;
          }
        }
      } else { // scenario when we no user config's where available.
        searchStatusMsg = LdapConstants.NO_USER_CONFIG_FAILURE;
      }
    }

    return LdapResponse.builder().status(searchStatus).message(searchStatusMsg).build();
  }

  LdapResponse validateGroupConfig(LdapGroupConfig config) {
    LdapResponse connectionTestResponse = validateConnectionConfig();
    if (Status.FAILURE == connectionTestResponse.getStatus()) {
      return connectionTestResponse;
    }

    String message = null;
    Status status = Status.FAILURE;
    LdapListGroupsResponse ldapListGroupsResponses = null;
    try {
      ldapListGroupsResponses = listGroups(Arrays.asList(config), null, LdapConstants.MIN_GROUP_QUERY_SIZE).get(0);
    } catch (LdapException e) {
      log.error("Ldap validate group config failed for: [{}]", config.getNameAttr(), e);
      message = e.getResultCode().toString();
    }

    if (ldapListGroupsResponses != null) {
      if (Status.FAILURE == ldapListGroupsResponses.getLdapResponse().getStatus()
          || (ldapListGroupsResponses.getSearchResult() != null
              && ldapListGroupsResponses.getSearchResult().size() == 0)) {
        message = LdapConstants.GROUP_CONFIG_FAILURE;
      } else {
        status = Status.SUCCESS;
        message = LdapConstants.GROUP_CONFIG_SUCCESS;
      }
    }

    return LdapResponse.builder().status(status).message(message).build();
  }

  private LdapSearch.Builder getDefaultLdapSearchBuilder(LdapSearchConfig config) {
    return LdapSearch.builder()
        .baseDN(config.getBaseDN())
        .connectionFactory(getConnectionFactory())
        .referralsEnabled(connectionConfig.isReferralsEnabled())
        .maxReferralHops(connectionConfig.getMaxReferralHops());
  }

  private LdapUserConfig userExists(List<LdapUserSettings> userConfigs, String identifier) {
    LdapUserConfig ldapUserConfig = null;
    List<LdapUserExistsRequest> searchDnResolverList = null;
    if (CollectionUtils.isNotEmpty(userConfigs)) {
      searchDnResolverList =
          userConfigs.stream()
              .map(userConfig -> {
                LdapSearch search =
                    getDefaultLdapSearchBuilder(userConfig).searchFilter(userConfig.getSearchFilter()).build();
                return new LdapUserExistsRequest(userConfig, search, identifier,
                    connectionConfig.getConnectTimeout() + connectionConfig.getResponseTimeout());
              })
              .collect(Collectors.toList());

      AbstractLdapResponse abstractLdapResponse =
          ldapParallelSearchExecutor.userExist(searchDnResolverList, executeLdapUserExistsRequest);

      if (abstractLdapResponse != null && abstractLdapResponse.getLdapResponse().getStatus() == Status.SUCCESS) {
        ldapUserConfig = abstractLdapResponse.getLdapUserConfig();
      }
    }

    return ldapUserConfig;
  }

  private List<LdapListGroupsResponse> listGroups(List<? extends LdapGroupConfig> ldapGroupConfigs,
      String additionalFilter, int limit, String... returnFields) throws LdapException {
    List<LdapListGroupsResponse> ldapListGroupsResponses = Lists.newArrayList();
    if (CollectionUtils.isNotEmpty(ldapGroupConfigs)) {
      List<LdapListGroupsRequest> ldapGetUsersRequests =
          getLdapGetUsersRequests(ldapGroupConfigs, additionalFilter, limit, returnFields);
      ldapListGroupsResponses =
          ldapParallelSearchExecutor.listGroupsSearchResult(ldapGetUsersRequests, executeLdapGroupsSearchRequest);
    }
    return ldapListGroupsResponses;
  }

  private List<LdapListGroupsRequest> getLdapGetUsersRequests(
      List<? extends LdapGroupConfig> ldapGroupConfigs, String additionalFilter, int limit, String... returnFields) {
    return ldapGroupConfigs.stream()
        .map(groupConfig -> {
          LdapSearch search = getDefaultLdapSearchBuilder(groupConfig)
                                  .limit(limit)
                                  .searchFilter(groupConfig.getFilter(additionalFilter))
                                  .build();
          return new LdapListGroupsRequest(search, returnFields, groupConfig,
              connectionConfig.getConnectTimeout() + connectionConfig.getResponseTimeout());
        })
        .collect(Collectors.toList());
  }

  private List<LdapListGroupsResponse> listGroupsByName(List<LdapGroupSettings> groupConfig, String name)
      throws LdapException {
    return listGroups(groupConfig, String.format("*%s*", name), LdapConstants.MAX_GROUP_SEARCH_SIZE);
  }

  List<LdapGetUsersResponse> listGroupUsers(LdapSettings settings, List<String> groupDnList) {
    List<LdapGetUsersResponse> ldapGetUsersResponse = new ArrayList<>();
    List<LdapGetUsersRequest> ldapGetUsersRequests;
    List<? extends LdapUserConfig> ldapUserConfigs = settings.getUserSettingsList();
    if (!Collections.isEmpty(ldapUserConfigs)) {
      ldapGetUsersRequests =
          groupDnList.stream()
              .flatMap(groupDn -> ldapUserConfigs.stream().map(userConfig -> {
                LdapSearch search = getDefaultLdapSearchBuilder(userConfig)
                                        .searchFilter(userConfig.getGroupMembershipFilter(groupDn))
                                        .fallBackSearchFilter(userConfig.getFallbackGroupMembershipFilter(groupDn))
                                        .build();
                return new LdapGetUsersRequest(userConfig, search,
                    connectionConfig.getConnectTimeout() + connectionConfig.getResponseTimeout(), groupDn,
                    settings.isUseOnlyFallBackMechanism());
              }))
              .collect(Collectors.toList());

      ldapGetUsersResponse =
          ldapParallelSearchExecutor.getUserSearchResult(ldapGetUsersRequests, executeLdapGetUsersRequest);
    } else {
      log.warn("No user config passed to listGroupUsers method for groupDn = {} ", groupDnList.toString());
    }

    return ldapGetUsersResponse;
  }

  void populateGroupSize(SearchResult groups, LdapSettings ldapSettings) throws LdapException {
    long startTime = System.currentTimeMillis();
    List<String> groupDnList = groups.getEntries().stream().map(LdapEntry::getDn).collect(Collectors.toList());
    List<LdapGetUsersResponse> ldapGetUsersResponses = listGroupUsers(ldapSettings, groupDnList);

    groups.getEntries().forEach(ldapEntry -> {
      log.info("Ldap Entry = [{}]", ldapEntry.toString());
      int groupSize =
          ldapGetUsersResponses.stream()
              .filter(ldapGetUsersResponse -> ldapEntry.getDn().equals(ldapGetUsersResponse.getGroupBaseDn()))
              .mapToInt(ldapGetUsersResponse
                  -> ldapGetUsersResponse.getSearchResult() != null ? ldapGetUsersResponse.getSearchResult().size() : 0)
              .sum();
      LdapAttribute groupSizeAttr = new LdapAttribute("groupSize", String.valueOf(groupSize));
      ldapEntry.addAttribute(groupSizeAttr);
    });
    long elapsedTime = System.currentTimeMillis() - startTime;
    log.info("elapsedTime : {}", elapsedTime);
  }

  List<LdapListGroupsResponse> searchGroupsByName(List<LdapGroupSettings> groupConfig, String name)
      throws LdapException {
    return listGroupsByName(groupConfig, name);
  }

  LdapListGroupsResponse getGroupByDn(List<LdapGroupSettings> groupConfigs, String dn) throws LdapException {
    LdapListGroupsResponse listGroupsResponse = null;
    for (LdapGroupSettings groupConfig : groupConfigs) {
      String oldBaseDn = groupConfig.getBaseDN();
      groupConfig.setBaseDN(dn);
      List<LdapListGroupsResponse> ldapListGroupsResponses =
          listGroups(Arrays.asList(groupConfig), null, 1, groupConfig.getReturnAttrs());

      // There could only be one groups since the base dn is given and we are passing limit as 1.
      if (ldapListGroupsResponses.size() == 1) {
        LdapListGroupsResponse currentLdapListGroupsResponse = ldapListGroupsResponses.get(0);

        // if there are multiple responses with some failures some success but with no result and success with result.
        if (Status.SUCCESS == currentLdapListGroupsResponse.getLdapResponse().getStatus()) {
          listGroupsResponse = currentLdapListGroupsResponse;
          groupConfig.setBaseDN(oldBaseDn);
          break;
        }
      }
      groupConfig.setBaseDN(oldBaseDn);
    }
    return listGroupsResponse;
  }

  public LdapResponse authenticate(List<LdapUserSettings> userConfigs, String identifier, String password) {
    LdapUserConfig ldapUserConfig = null;
    try {
      ldapUserConfig = userExists(userConfigs, identifier);
      if (ldapUserConfig == null) {
        return LdapResponse.builder().status(Status.FAILURE).message(LdapConstants.USER_NOT_FOUND).build();
      }

      LdapSearch search = LdapSearch.builder()
                              .baseDN(ldapUserConfig.getBaseDN())
                              .connectionFactory(getConnectionFactory())
                              .searchFilter(ldapUserConfig.getSearchFilter())
                              .referralsEnabled(connectionConfig.isReferralsEnabled())
                              .maxReferralHops(connectionConfig.getMaxReferralHops())
                              .build();

      SearchDnResolver resolver = search.getSearchDnResolver(ldapUserConfig.getUserFilter());

      BindAuthenticationHandler handler = new BindAuthenticationHandler(getConnectionFactory());

      Authenticator authenticator = new Authenticator(resolver, handler);

      AuthenticationRequest request = new AuthenticationRequest(identifier, new Credential(password));
      AuthenticationResponse response = authenticator.authenticate(request);
      log.info("LAT: Authentication response: [{}], ", response.toString());
      if (response.getResult()) {
        return LdapResponse.builder().status(Status.SUCCESS).message(LdapConstants.AUTHENTICATION_SUCCESS).build();
      } else {
        if (response.getResultCode() != null && response.getAuthenticationResultCode() != null) {
          log.info("LDAP auth failed with response: identifier:[{}],authenticationResultCode:[{}],resultCode:[{}]",
              identifier, response.getAuthenticationResultCode(), response.getResultCode().name());
        } else if (response.getResultCode() != null) {
          log.info("LDAP auth failed with response: identifier:[{}],resultCode:[{}]", identifier,
              response.getResultCode().name());
        } else if (response.getAuthenticationResultCode() != null) {
          log.info("LDAP auth failed with response: identifier:[{}],authenticationResultCode:[{}]", identifier,
              response.getAuthenticationResultCode());
        } else {
          log.info("LDAP auth failed, response not available for identifier:[{}]", identifier);
        }
        return LdapResponse.builder().status(Status.FAILURE).message(LdapConstants.INVALID_CREDENTIALS).build();
      }
    } catch (LdapException e) {
      log.error("Ldap authentication failed for identifier: [{}] and Ldap display Name: [{}]", identifier,
          ldapUserConfig.getDisplayNameAttr(), e);
      return LdapResponse.builder()
          .status(Status.FAILURE)
          .message(e.getResultCode() == null ? LdapConstants.INVALID_CREDENTIALS : e.getResultCode().toString())
          .build();
    }
  }
}
