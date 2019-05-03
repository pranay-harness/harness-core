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

@Slf4j
public class EnvironmentConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLEnvironmentConnection, QLEnvironmentsQueryParameters> {
  @Override
  public QLEnvironmentConnection fetch(QLEnvironmentsQueryParameters qlQuery) {
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
