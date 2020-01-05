package io.harness;

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
    User user = User.Builder.anUser().withUuid("user1Id").build();
    UserGroup userGroup = authHandler.buildDefaultAdminUserGroup(accountId, user);
    UserPermissionInfo userPermissionInfo =
        authHandler.evaluateUserPermissionInfo(accountId, Arrays.asList(userGroup), user);
    return ExecutionInput.newExecutionInput()
        .query(query)
        .dataLoaderRegistry(getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("accountId", accountId, "permissions", userPermissionInfo))
        .build();
  }
}