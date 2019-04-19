package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;

import java.util.stream.Collectors;

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
    PagedData<QLApplication> pagedData = PagedData.<QLApplication>builder().build();
    int limit = getPageLimit(dataFetchingEnvironment);
    int offset = getPageOffset(dataFetchingEnvironment);

    // TODO - remove this once accountID is available from the authentication
    String accountID = (String) getArgumentValue(dataFetchingEnvironment, GraphQLConstants.ACCOUNT_ID_ARG);
    if (StringUtils.isBlank(accountID)) {
      addInvalidInputInfo(pagedData, GraphQLConstants.ACCOUNT_ID_ARG);
      return pagedData;
    }
    PageRequest<Application> pageRequest = PageRequestBuilder.aPageRequest()
                                               .addFilter(Application.ACCOUNT_ID_KEY, Operator.EQ, accountID)
                                               .withLimit(String.valueOf(limit))
                                               .withOffset(String.valueOf(offset))
                                               .build();

    final PageResponse<Application> applications = appService.list(pageRequest);
    return PagedData.<QLApplication>builder()
        .offset(offset)
        .limit(limit)
        .total(applications.getTotal())
        .data(applications.getResponse()
                  .stream()
                  .map(application -> ApplicationController.getApplicationInfo(application))
                  .collect(Collectors.toList()))
        .build();
  }
}
