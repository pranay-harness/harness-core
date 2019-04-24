package software.wings.graphql.datafetcher.environment;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentsDataFetcher extends AbstractDataFetcher<PagedData<QLEnvironment>> {
  EnvironmentService environmentService;
  AuthHandler authHandler;

  @Inject
  public EnvironmentsDataFetcher(AuthHandler authHandler, EnvironmentService environmentService) {
    super(authHandler);
    this.environmentService = environmentService;
  }

  @Override
  public PagedData<QLEnvironment> fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
