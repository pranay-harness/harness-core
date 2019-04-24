package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class ApplicationDataFetcher extends AbstractDataFetcher<QLApplication> {
  @Inject HPersistence persistence;

  @Inject
  public ApplicationDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLApplication fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLApplicationQueryParameters qlQuery = fetchParameters(QLApplicationQueryParameters.class, dataFetchingEnvironment);

    Application application = persistence.get(Application.class, qlQuery.getApplicationId());
    if (application == null) {
      throw new InvalidRequestException("Application does not exist", WingsException.USER);
    }

    final QLApplicationBuilder builder = QLApplication.builder();
    ApplicationController.populateApplication(application, builder);
    return builder.build();
  }
}
