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
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

import java.util.concurrent.ExecutionException;
import javax.validation.constraints.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationDataFetcher extends AbstractDataFetcher<QLApplication> {
  AppService appService;

  @Inject
  public ApplicationDataFetcher(AppService appService, AuthHandler authHandler) {
    super(authHandler);
    this.appService = appService;
  }

  @Override
  public QLApplication fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    String appId = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.APP_ID);

    if (StringUtils.isBlank(appId)) {
      QLApplication applicationInfo = QLApplication.builder().build();
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

  private QLApplication loadApplicationInfo(String appId) {
    QLApplication applicationInfo;
    Application application = appService.get(appId);
    if (null == application) {
      applicationInfo = QLApplication.builder().build();
      addNoRecordFoundInfo(applicationInfo, GraphQLConstants.APP_ID);
    } else {
      applicationInfo = ApplicationController.getApplicationInfo(application);
    }
    return applicationInfo;
  }

  private QLApplication loadApplicationInfoWithBatching(
      @NotNull String appId, DataLoader<String, QLApplication> dataLoader) {
    try {
      return dataLoader.load(appId).get();
    } catch (InterruptedException | ExecutionException e) {
      throw batchFetchException(e);
    }
  }
}
