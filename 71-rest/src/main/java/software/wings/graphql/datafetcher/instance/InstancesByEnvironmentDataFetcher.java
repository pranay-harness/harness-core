package software.wings.graphql.datafetcher.instance;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLInstancesByEnvironmentQueryParameters;
import software.wings.graphql.schema.type.QLInstance;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.QLInstanceConnection;
import software.wings.graphql.schema.type.QLInstanceConnection.QLInstanceConnectionBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

public class InstancesByEnvironmentDataFetcher extends AbstractConnectionDataFetcher<QLInstanceConnection> {
  @Inject
  public InstancesByEnvironmentDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  protected QLInstanceConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLInstancesByEnvironmentQueryParameters qlQuery =
        fetchParameters(QLInstancesByEnvironmentQueryParameters.class, dataFetchingEnvironment);

    final Query<Instance> query = persistence.createQuery(Instance.class)
                                      .filter(InstanceKeys.envId, qlQuery.getEnvId())
                                      .filter(InstanceKeys.isDeleted, false)
                                      .order(Sort.descending(InstanceKeys.lastDeployedAt));

    QLInstanceConnectionBuilder connectionBuilder = QLInstanceConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, instance -> {
      QLInstanceBuilder builder = QLInstance.builder();
      InstanceController.populateInstance(instance, builder);
      connectionBuilder.node(builder.build());
    }));

    return connectionBuilder.build();
  }
}
