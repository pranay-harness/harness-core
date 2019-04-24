package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationsDataFetcher extends AbstractDataFetcher<PagedData<QLApplication>> {
  AppService appService;

  @Inject
  public ApplicationsDataFetcher(AppService appService, AuthHandler authHandler) {
    super(authHandler);
    this.appService = appService;
  }

  @Override
  public PagedData<QLApplication> fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
