package software.wings.graphql.datafetcher.environment;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLEnvironmentsQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLEnvironmentConnection;
import software.wings.graphql.schema.type.QLEnvironmentConnection.QLEnvironmentConnectionBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class EnvironmentConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLEnvironmentConnection, QLEnvironmentsQueryParameters> {
  @Override
  @AuthRule(permissionType = PermissionType.ENV, action = Action.READ)
  public QLEnvironmentConnection fetchConnection(QLEnvironmentsQueryParameters qlQuery) {
    final Query<Environment> query = persistence.createAuthorizedQuery(Environment.class)
                                         .filter(EnvironmentKeys.appId, qlQuery.getApplicationId())
                                         .order(Sort.descending(EnvironmentKeys.createdAt));

    QLEnvironmentConnectionBuilder connectionBuilder = QLEnvironmentConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, environment -> {
      QLEnvironmentBuilder builder = QLEnvironment.builder();
      EnvironmentController.populateEnvironment(environment, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }
}
