package software.wings.graphql.datafetcher.service;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLServicesQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.graphql.schema.type.QLServiceConnection;
import software.wings.graphql.schema.type.QLServiceConnection.QLServiceConnectionBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class ServiceConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLServiceConnection, QLServicesQueryParameters> {
  @Override
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  protected QLServiceConnection fetchConnection(QLServicesQueryParameters qlQuery) {
    final Query<Service> query = persistence.createAuthorizedQuery(Service.class)
                                     .filter(ServiceKeys.appId, qlQuery.getApplicationId())
                                     .order(Sort.descending(ServiceKeys.createdAt));

    QLServiceConnectionBuilder qlServiceConnectionBuilder = QLServiceConnection.builder();
    qlServiceConnectionBuilder.pageInfo(populate(qlQuery, query, service -> {
      QLServiceBuilder builder = QLService.builder();
      ServiceController.populateService(service, builder);
      qlServiceConnectionBuilder.node(builder.build());
    }));

    return qlServiceConnectionBuilder.build();
  }
}
