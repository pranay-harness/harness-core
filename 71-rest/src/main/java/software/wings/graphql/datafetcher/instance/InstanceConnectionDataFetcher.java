package software.wings.graphql.datafetcher.instance;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLInstanceConnection;
import software.wings.graphql.schema.type.QLInstanceConnection.QLInstanceConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilterType;
import software.wings.graphql.schema.type.instance.QLInstance;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class InstanceConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLInstanceFilter, QLNoOpSortCriteria, QLInstanceConnection> {
  @Inject private InstanceControllerManager instanceControllerManager;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLInstanceConnection fetchConnection(List<QLInstanceFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Instance> query = populateFilters(wingsPersistence, filters, Instance.class)
                                .filter(InstanceKeys.isDeleted, false)
                                .order(Sort.descending(InstanceKeys.lastDeployedAt));

    QLInstanceConnectionBuilder connectionBuilder = QLInstanceConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, instance -> {
      QLInstance qlInstance = instanceControllerManager.getQLInstance(instance);
      connectionBuilder.node(qlInstance);
    }));

    return connectionBuilder.build();
  }

  @Override
  protected String getFilterFieldName(String filterType) {
    QLInstanceFilterType type = QLInstanceFilterType.valueOf(filterType);
    switch (type) {
      case CreatedAt:
        return "createdAt";
      case Application:
        return "appId";
      case Service:
        return "serviceId";
      case Environment:
        return "envId";
      case CloudProvider:
        return "computeProviderId";
      case InstanceType:
        return "instanceType";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
