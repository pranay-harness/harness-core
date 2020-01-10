package io.harness;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.wings.graphql.utils.GraphQLConstants.GRAPHQL_QUERY_STRING;
import static software.wings.graphql.utils.GraphQLConstants.HTTP_SERVLET_REQUEST;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.graphql.GraphQLTestMixin;
import org.dataloader.DataLoaderRegistry;
import org.junit.Rule;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.security.auth.AuthHandler;

import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

public abstract class GraphQLTest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;
  @Inject AuthHandler authHandler;

  @Override
  public GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistryHelper.getDataLoaderRegistry();
  }

  @Override
  public ExecutionInput getExecutionInput(String query, String accountId) {
    User user = User.Builder.anUser().uuid("user1Id").build();
    UserGroup userGroup = authHandler.buildDefaultAdminUserGroup(accountId, user);
    UserPermissionInfo userPermissionInfo =
        authHandler.evaluateUserPermissionInfo(accountId, Arrays.asList(userGroup), user);
    return ExecutionInput.newExecutionInput()
        .query(query)
        .dataLoaderRegistry(getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("accountId", accountId, "permissions", userPermissionInfo,
            HTTP_SERVLET_REQUEST, getHttpServletRequest(accountId), GRAPHQL_QUERY_STRING, query))
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