package software.wings.graphql.datafetcher.environment;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class EnvironmentConnectionDataFetcher extends AbstractConnectionDataFetcher<QLEnvironmentConnection> {
  @Inject
  public EnvironmentConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  private static final PermissionAttribute permissionAttribute =
      new PermissionAttribute(PermissionType.PIPELINE, Action.READ);

  @Override
  public QLEnvironmentConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLEnvironmentsQueryParameters qlQuery =
        fetchParameters(QLEnvironmentsQueryParameters.class, dataFetchingEnvironment);

    final Query<Environment> query = persistence.createQuery(Environment.class)
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
