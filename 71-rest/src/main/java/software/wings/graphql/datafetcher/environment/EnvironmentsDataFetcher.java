package software.wings.graphql.datafetcher.environment;

import static software.wings.beans.Environment.ENVIRONMENT_TYPE_KEY;
import static software.wings.graphql.utils.GraphQLConstants.APP_ID_ARG;
import static software.wings.graphql.utils.GraphQLConstants.ENV_TYPE_ARG;

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
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.PagedData;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

import java.util.stream.Collectors;

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
    PagedData<QLEnvironment> pagedData = PagedData.<QLEnvironment>builder().build();
    String appId = (String) getArgumentValue(dataFetchingEnvironment, APP_ID_ARG);
    String environmentType = (String) getArgumentValue(dataFetchingEnvironment, ENV_TYPE_ARG);

    if (StringUtils.isEmpty(appId)) {
      addInvalidInputInfo(pagedData, APP_ID_ARG);
      return pagedData;
    }

    int limit = getPageLimit(dataFetchingEnvironment);
    int offset = getPageOffset(dataFetchingEnvironment);
    PageRequestBuilder pageRequestBuilder = PageRequestBuilder.aPageRequest()
                                                .withOffset(String.valueOf(offset))
                                                .withLimit(String.valueOf(limit))
                                                .addFilter(Environment.APP_ID_KEY, Operator.EQ, appId);

    if (!StringUtils.isEmpty(environmentType)) {
      pageRequestBuilder.addFilter(ENVIRONMENT_TYPE_KEY, Operator.EQ, environmentType);
    }
    PageRequest<Environment> pageRequest = pageRequestBuilder.build();
    PageResponse<Environment> result = environmentService.list(pageRequest, false);

    return PagedData.<QLEnvironment>builder()
        .total(result.getTotal())
        .data(result.getResponse()
                  .stream()
                  .map(env -> EnvironmentController.getEnvironmentInfo(env))
                  .collect(Collectors.toList()))
        .limit(Integer.parseInt(result.getLimit()))
        .offset(Integer.parseInt(result.getOffset()))
        .build();
  }
}
