package software.wings.graphql.datafetcher.service;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class ServiceConnectionDataFetcher extends AbstractConnectionDataFetcher<QLServiceConnection> {
  @Inject
  public ServiceConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  protected QLServiceConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLServicesQueryParameters qlQuery = fetchParameters(QLServicesQueryParameters.class, dataFetchingEnvironment);
    final Query<Service> query = persistence.createQuery(Service.class)
                                     .filter(ServiceKeys.appId, qlQuery.getAppId())
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
