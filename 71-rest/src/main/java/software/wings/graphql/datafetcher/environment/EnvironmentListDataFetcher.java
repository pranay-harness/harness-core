package software.wings.graphql.datafetcher.environment;

import static software.wings.beans.Environment.ENVIRONMENT_TYPE_KEY;
import static software.wings.graphql.utils.GraphQLConstants.APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.ENV_TYPE;

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
import software.wings.graphql.schema.type.EnvironmentInfo;
import software.wings.graphql.schema.type.PagedData;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentListDataFetcher extends AbstractDataFetcher<PagedData<EnvironmentInfo>> {
  EnvironmentService environmentService;
  AuthHandler authHandler;
  EnvironmentAdaptor environmentAdaptor;

  @Inject
  public EnvironmentListDataFetcher(
      AuthHandler authHandler, EnvironmentService environmentService, EnvironmentAdaptor environmentAdaptor) {
    super(authHandler);
    this.environmentService = environmentService;
    this.environmentAdaptor = environmentAdaptor;
  }

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    PagedData<EnvironmentInfo> pagedData = PagedData.<EnvironmentInfo>builder().build();
    String appId = (String) getArgumentValue(dataFetchingEnvironment, APP_ID);
    String environmentType = (String) getArgumentValue(dataFetchingEnvironment, ENV_TYPE);

    if (StringUtils.isEmpty(appId)) {
      addInvalidInputInfo(pagedData, APP_ID);
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

    return PagedData.<EnvironmentInfo>builder()
        .total(result.getTotal())
        .data(result.getResponse()
                  .stream()
                  .map(env -> environmentAdaptor.getEnvironmentInfo(env))
                  .collect(Collectors.toList()))
        .limit(Integer.parseInt(result.getLimit()))
        .offset(Integer.parseInt(result.getOffset()))
        .build();
  }
}
