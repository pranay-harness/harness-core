package software.wings.graphql.datafetcher.service;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.graphql.schema.type.QLServiceConnection;
import software.wings.graphql.schema.type.QLServiceConnection.QLServiceConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class ServiceConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLServiceFilter, QLNoOpSortCriteria, QLServiceConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  protected QLServiceConnection fetchConnection(List<QLServiceFilter> serviceFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Service> query =
        populateFilters(wingsPersistence, serviceFilters, Service.class).order(Sort.descending(ServiceKeys.createdAt));

    QLServiceConnectionBuilder qlServiceConnectionBuilder = QLServiceConnection.builder();
    qlServiceConnectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, service -> {
      QLServiceBuilder builder = QLService.builder();
      ServiceController.populateService(service, builder);
      qlServiceConnectionBuilder.node(builder.build());
    }));

    return qlServiceConnectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLServiceFilterType serviceFilterType = QLServiceFilterType.valueOf(filterType);
    switch (serviceFilterType) {
      case Application:
        return ServiceKeys.appId;
      case Service:
        return ServiceKeys.uuid;
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
