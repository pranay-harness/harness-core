package software.wings.graphql.datafetcher.instance;

import static io.harness.govern.Switch.noop;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLInstanceConnectionQueryParameters;
import software.wings.graphql.schema.type.QLInstanceConnection;
import software.wings.graphql.schema.type.QLInstanceConnection.QLInstanceConnectionBuilder;
import software.wings.graphql.schema.type.instance.QLInstance;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class InstanceConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLInstanceConnection, QLInstanceConnectionQueryParameters> {
  @Inject private InstanceControllerManager instanceControllerManager;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLInstanceConnection fetchConnection(QLInstanceConnectionQueryParameters qlQuery) {
    final Query<Instance> query = persistence.createAuthorizedQuery(Instance.class)
                                      .filter(InstanceKeys.isDeleted, false)
                                      .order(Sort.descending(InstanceKeys.lastDeployedAt));

    if (qlQuery.getEnvironmentId() != null) {
      query.filter(InstanceKeys.envId, qlQuery.getEnvironmentId());
    }

    if (qlQuery.getServiceId() != null) {
      query.filter(InstanceKeys.serviceId, qlQuery.getServiceId());
    }

    if (qlQuery.getEnvType() != null) {
      addEnvTypeFilter(query, qlQuery.getEnvType());
    }

    QLInstanceConnectionBuilder connectionBuilder = QLInstanceConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, instance -> {
      QLInstance qlInstance = instanceControllerManager.getQLInstance(instance);
      connectionBuilder.node(qlInstance);
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
        noop();
    }
  }
}
