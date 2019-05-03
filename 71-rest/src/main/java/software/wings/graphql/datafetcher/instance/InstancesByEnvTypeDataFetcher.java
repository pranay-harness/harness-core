package software.wings.graphql.datafetcher.instance;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLInstancesByEnvTypeQueryParameters;
import software.wings.graphql.schema.type.QLInstance;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.QLInstanceConnection;
import software.wings.graphql.schema.type.QLInstanceConnection.QLInstanceConnectionBuilder;

@Slf4j
public class InstancesByEnvTypeDataFetcher
    extends AbstractConnectionDataFetcher<QLInstanceConnection, QLInstancesByEnvTypeQueryParameters> {
  @Override
  protected QLInstanceConnection fetch(QLInstancesByEnvTypeQueryParameters qlQuery) {
    final Query<Instance> query = persistence.createQuery(Instance.class)
                                      .filter(InstanceKeys.isDeleted, false)
                                      .filter(InstanceKeys.accountId, qlQuery.getAccountId())
                                      .order(Sort.descending(InstanceKeys.lastDeployedAt));

    addEnvTypeFilter(query, qlQuery.getEnvType());

    QLInstanceConnectionBuilder connectionBuilder = QLInstanceConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, instance -> {
      QLInstanceBuilder builder = QLInstance.builder();
      InstanceController.populateInstance(instance, builder);
      connectionBuilder.node(builder.build());
    }));

    return connectionBuilder.build();
  }

  private void addEnvTypeFilter(Query<Instance> query, EnvironmentType envType) {
    switch (envType) {
      case PROD:
      case NON_PROD:
        query.filter(InstanceKeys.envType, envType);
        break;
      default:
        logger.debug("Not adding any envType filter");
    }
  }
}
