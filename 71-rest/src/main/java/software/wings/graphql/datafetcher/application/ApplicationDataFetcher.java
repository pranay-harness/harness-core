package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.ApplicationInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

import javax.validation.constraints.NotNull;

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
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    String appId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.APP_ID);

    if (StringUtils.isBlank(appId)) {
      ApplicationInfo applicationInfo = ApplicationInfo.builder().build();
      addInvalidInputInfo(applicationInfo, GraphQLConstants.APP_ID);
      return applicationInfo;
    }

    String batchDataLoader = getBatchedDataLoaderName();
    if (StringUtils.isBlank(batchDataLoader)) {
      return loadApplicationInfo(appId);
    } else {
      return loadApplicationInfoWithBatching(appId, dataFetchingEnvironment.getDataLoader(batchDataLoader));
    }
  }

  private Object loadApplicationInfo(String appId) {
    ApplicationInfo applicationInfo;
    Application application = appService.get(appId);
    if (null == application) {
      applicationInfo = ApplicationInfo.builder().build();
      addNoRecordFoundInfo(applicationInfo, GraphQLConstants.APP_ID);
    } else {
      applicationInfo = applicationAdaptor.getApplicationInfo(application);
    }
    return applicationInfo;
  }

  private Object loadApplicationInfoWithBatching(@NotNull String appId, DataLoader<String, Object> dataLoader) {
    return dataLoader.load(appId);
  }
}
