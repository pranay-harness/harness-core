package software.wings.graphql.datafetcher.application;

import static software.wings.graphql.datafetcher.QueryOperationsEnum.APPLICATION;
import static software.wings.graphql.datafetcher.QueryOperationsEnum.APPLICATIONS;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.ApplicationInfo;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ApplicationDataFetcher extends AbstractDataFetcher {
  private AppService appService;
  private ApplicationAdaptor applicationAdaptor;

  @Inject
  public ApplicationDataFetcher(AppService appService, AuthHandler authHandler, ApplicationAdaptor applicationAdaptor) {
    super(authHandler);
    this.appService = appService;
    this.applicationAdaptor = applicationAdaptor;
  }

  public DataFetcher<ApplicationInfo> getApp() {
    return environment -> {
      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      Application application = appService.get(appId);
      ApplicationInfo applicationInfo = ApplicationInfo.builder().build();
      if (null == application) {
        addNoRecordFoundInfo(applicationInfo, GraphQLConstants.APP_ID);
        return applicationInfo;
      }
      return applicationAdaptor.getApplicationInfo(application);
    };
  }

  public DataFetcher<PagedData<ApplicationInfo>> getApps() {
    return environment -> {
      PagedData<ApplicationInfo> pagedData = PagedData.<ApplicationInfo>builder().build();
      PageResponse<Application> pageResponse = null;
      int limit = getPageLimit(environment);
      int offset = getPageOffset(environment);

      // TODO - remove this once accountID is available from the authentication
      String accountID = environment.getArgument(GraphQLConstants.ACCOUNT_ID);
      if (StringUtils.isBlank(accountID)) {
        addInvalidInputInfo(pagedData, GraphQLConstants.ACCOUNT_ID);
        return pagedData;
      }
      PageRequest<Application> pageRequest = PageRequestBuilder.aPageRequest()
                                                 .addFilter(Application.ACCOUNT_ID_KEY, Operator.EQ, accountID)
                                                 .withLimit(String.valueOf(limit))
                                                 .withOffset(String.valueOf(offset))
                                                 .build();

      final PageResponse<Application> applications = appService.list(pageRequest);
      return PagedData.<ApplicationInfo>builder()
          .offset(offset)
          .limit(limit)
          .total(applications.getTotal())
          .data(applications.getResponse()
                    .stream()
                    .map(application -> applicationAdaptor.getApplicationInfo(application))
                    .collect(Collectors.toList()))
          .build();
    };
  }

  @Override
  public Map<String, DataFetcher<?>> getOperationToDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .put(APPLICATIONS.getOperationName(), getApps())
        .put(APPLICATION.getOperationName(), getApp())
        .build();
  }
}
