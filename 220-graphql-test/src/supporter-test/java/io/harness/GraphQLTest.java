/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static software.wings.graphql.utils.GraphQLConstants.GRAPHQL_QUERY_STRING;
import static software.wings.graphql.utils.GraphQLConstants.HTTP_SERVLET_REQUEST;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.ALL;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.graphql.GraphQLTestMixin;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UsageRestrictions;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.dataloader.DataLoaderRegistry;
import org.junit.Before;
import org.junit.Rule;

public abstract class GraphQLTest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;
  @Inject AuthHandler authHandler;
  @Inject AuthService authService;
  private String accountId;
  @Inject BigQueryService bigQueryService;
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  @Inject UserService userService;
  @Inject UsageRestrictionsService usageRestrictionsService;

  @Override
  public GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }

  public String getAccountId() {
    return accountId;
  }

  @Before
  public void setup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
  }

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistryHelper.getDataLoaderRegistry();
  }

  private UsageRestrictions getAllAppAllEnvRestriction() {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(ALL).build();
    EnvFilter envFilter = EnvFilter.builder().filterTypes(ImmutableSet.of(PROD)).build();
    EnvFilter envFilterNonPROD = EnvFilter.builder().filterTypes(ImmutableSet.of(NON_PROD)).build();
    UsageRestrictions.AppEnvRestriction appEnvRestrictionPROD =
        UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    UsageRestrictions.AppEnvRestriction appEnvRestrictionNONPROD =
        UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilterNonPROD).build();
    return UsageRestrictions.builder()
        .appEnvRestrictions(ImmutableSet.of(appEnvRestrictionPROD, appEnvRestrictionNONPROD))
        .build();
  }

  @Override
  public ExecutionInput getExecutionInput(String query, String accountId) {
    User user = User.Builder.anUser().uuid("user1Id").build();
    UserGroup userGroup = authHandler.buildDefaultAdminUserGroup(accountId, user);
    UserPermissionInfo userPermissionInfo =
        authHandler.evaluateUserPermissionInfo(accountId, Arrays.asList(userGroup), user);
    UserRestrictionInfo userRestrictionInfo =
        authService.getUserRestrictionInfo(accountId, user, userPermissionInfo, false);
    userRestrictionInfo.setAppEnvMapForUpdateAction(
        usageRestrictionsService.getAppEnvMapFromUserPermissions(accountId, userPermissionInfo, UPDATE));
    userRestrictionInfo.setAppEnvMapForReadAction(
        usageRestrictionsService.getAppEnvMapFromUserPermissions(accountId, userPermissionInfo, READ));
    userRestrictionInfo.setUsageRestrictionsForUpdateAction(getAllAppAllEnvRestriction());
    userRestrictionInfo.setUsageRestrictionsForReadAction(getAllAppAllEnvRestriction());
    return ExecutionInput.newExecutionInput()
        .query(query)
        .dataLoaderRegistry(getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("accountId", accountId, "permissions", userPermissionInfo,
            "restrictions", userRestrictionInfo, HTTP_SERVLET_REQUEST, getHttpServletRequest(accountId),
            GRAPHQL_QUERY_STRING, query))
        .build();
  }

  protected HttpServletRequest getHttpServletRequest(String accountId) {
    final HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    doReturn(new StringBuffer("https://app.harness.io/graphql")).when(mockHttpServletRequest).getRequestURL();
    doReturn(Collections.enumeration(ImmutableList.of("Authorization", "Cookies", API_KEY_HEADER)))
        .when(mockHttpServletRequest)
        .getHeaderNames();
    doReturn("cookie value").when(mockHttpServletRequest).getHeader("Cookies");
    doReturn("api_key_value").when(mockHttpServletRequest).getHeader(API_KEY_HEADER);
    doReturn("Bearer 12njdjksbkn").when(mockHttpServletRequest).getHeader("Authorization");
    return mockHttpServletRequest;
  }
}
