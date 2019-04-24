package software.wings.graphql.datafetcher.environment;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLEnvironmentQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class EnvironmentDataFetcher extends AbstractDataFetcher<QLEnvironment> {
  @Inject HPersistence persistence;

  @Inject
  public EnvironmentDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLEnvironment fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLEnvironmentQueryParameters qlQuery = fetchParameters(QLEnvironmentQueryParameters.class, dataFetchingEnvironment);

    Environment environment = persistence.get(Environment.class, qlQuery.getEnvironmentId());
    if (environment == null) {
      throw new InvalidRequestException("Environment does not exist", WingsException.USER);
    }

    final QLEnvironmentBuilder builder = QLEnvironment.builder();
    EnvironmentController.populateEnvironment(environment, builder);
    return builder.build();
  }
}
