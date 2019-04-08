package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.ApplicationInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationDataFetcher extends AbstractDataFetcher<ApplicationInfo> {
  AppService appService;
  ApplicationAdaptor applicationAdaptor;

  @Inject
  public ApplicationDataFetcher(AppService appService, AuthHandler authHandler, ApplicationAdaptor applicationAdaptor) {
    super(authHandler);
    this.appService = appService;
    this.applicationAdaptor = applicationAdaptor;
  }

  @Override
  public ApplicationInfo get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    String appId = dataFetchingEnvironment.getArgument(GraphQLConstants.APP_ID);
    Application application = appService.get(appId);
    ApplicationInfo applicationInfo = ApplicationInfo.builder().build();
    if (null == application) {
      addNoRecordFoundInfo(applicationInfo, GraphQLConstants.APP_ID);
      return applicationInfo;
    }
    return applicationAdaptor.getApplicationInfo(application);
  }
}
