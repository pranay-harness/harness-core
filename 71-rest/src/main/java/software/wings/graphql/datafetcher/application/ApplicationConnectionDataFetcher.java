package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLApplicationsQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.graphql.schema.type.QLApplicationConnection;
import software.wings.graphql.schema.type.QLApplicationConnection.QLApplicationConnectionBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class ApplicationConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLApplicationConnection, QLApplicationsQueryParameters> {
  @Inject
  public ApplicationConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLApplicationConnection fetch(QLApplicationsQueryParameters qlQuery) {
    final Query<Application> query = persistence.createQuery(Application.class)
                                         .filter(ApplicationKeys.accountId, qlQuery.getAccountId())
                                         .order(Sort.descending(ApplicationKeys.createdAt));

    QLApplicationConnectionBuilder connectionBuilder = QLApplicationConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, application -> {
      QLApplicationBuilder builder = QLApplication.builder();
      ApplicationController.populateApplication(application, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }
}
