package software.wings.graphql.datafetcher.environment;

import static software.wings.beans.Environment.APP_ID_KEY;
import static software.wings.beans.Environment.ENVIRONMENT_TYPE_KEY;
import static software.wings.graphql.utils.GraphQLConstants.APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.ENV_ID;
import static software.wings.graphql.utils.GraphQLConstants.ENV_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.QueryOperationsEnum;
import software.wings.graphql.schema.type.EnvironmentInfo;
import software.wings.graphql.schema.type.PagedData;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentDataFetcher extends AbstractDataFetcher {
  EnvironmentService environmentService;
  AuthHandler authHandler;
  EnvironmentAdaptor environmentAdaptor;

  @Inject
  public EnvironmentDataFetcher(
      AuthHandler authHandler, EnvironmentService environmentService, EnvironmentAdaptor environmentAdaptor) {
    super(authHandler);
    this.environmentService = environmentService;
    this.environmentAdaptor = environmentAdaptor;
  }

  public DataFetcher<PagedData<EnvironmentInfo>> getEnvironments() {
    return environment -> {
      PagedData<EnvironmentInfo> pagedData = PagedData.<EnvironmentInfo>builder().build();
      String appId = environment.getArgument(APP_ID);

      String environmentType = environment.getArgument(ENV_TYPE);

      if (StringUtils.isEmpty(appId)) {
        addInvalidInputInfo(pagedData, APP_ID);
        return pagedData;
      }

      PageResponse<Application> pageResponse = null;
      int limit = getPageLimit(environment);
      int offset = getPageOffset(environment);
      PageRequestBuilder pageRequestBuilder = PageRequestBuilder.aPageRequest()
                                                  .withOffset(String.valueOf(offset))
                                                  .withLimit(String.valueOf(limit))
                                                  .addFilter(APP_ID_KEY, Operator.EQ, appId);

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
          .limit(Integer.valueOf(result.getLimit()))
          .offset(Integer.valueOf(result.getOffset()))
          .build();
    };
  }

  public DataFetcher<EnvironmentInfo> getEnvironment() {
    return environment -> {
      String envId = environment.getArgument(ENV_ID);
      String appId = environment.getArgument(APP_ID);
      Environment env = environmentService.get(appId, envId);
      EnvironmentInfo environmentInfo = EnvironmentInfo.builder().build();
      if (null == env) {
        addNoRecordFoundInfo(environmentInfo, ENV_ID);
        return environmentInfo;
      }
      return environmentAdaptor.getEnvironmentInfo(env);
    };
  }

  @Override
  public Map<String, DataFetcher<?>> getOperationToDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .put(QueryOperationsEnum.ENVIRONMENTS.getOperationName(), getEnvironments())
        .put(QueryOperationsEnum.ENVIRONMENT.getOperationName(), getEnvironment())
        .build();
  }
}
