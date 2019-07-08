package software.wings.graphql.datafetcher.environment;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLEnvironmentConnection;
import software.wings.graphql.schema.type.QLEnvironmentConnection.QLEnvironmentConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class EnvironmentConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLEnvironmentFilter, QLNoOpSortCriteria, QLEnvironmentConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.ENV, action = Action.READ)
  public QLEnvironmentConnection fetchConnection(List<QLEnvironmentFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Environment> query =
        populateFilters(wingsPersistence, filters, Environment.class).order(Sort.descending(EnvironmentKeys.createdAt));

    QLEnvironmentConnectionBuilder connectionBuilder = QLEnvironmentConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, environment -> {
      QLEnvironmentBuilder builder = QLEnvironment.builder();
      EnvironmentController.populateEnvironment(environment, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLEnvironmentFilterType environmentFilterType = QLEnvironmentFilterType.valueOf(filterType);
    switch (environmentFilterType) {
      case Application:
        return EnvironmentKeys.appId;
      case Environment:
        return EnvironmentKeys.uuid;
      case EnvironmentType:
        return EnvironmentKeys.environmentType;
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
