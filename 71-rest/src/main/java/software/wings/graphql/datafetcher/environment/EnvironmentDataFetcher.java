package software.wings.graphql.datafetcher.environment;

import static software.wings.graphql.utils.GraphQLConstants.APP_ID_ARG;
import static software.wings.graphql.utils.GraphQLConstants.ENV_ID_ARG;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentDataFetcher extends AbstractDataFetcher<QLEnvironment> {
  EnvironmentService environmentService;
  AuthHandler authHandler;

  @Inject
  public EnvironmentDataFetcher(AuthHandler authHandler, EnvironmentService environmentService) {
    super(authHandler);
    this.environmentService = environmentService;
  }

  @Override
  public QLEnvironment fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    QLEnvironment environmentInfo = QLEnvironment.builder().build();
    String appId = (String) getArgumentValue(dataFetchingEnvironment, APP_ID_ARG);
    // Pre-checks
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(environmentInfo, GraphQLConstants.APP_ID_ARG);
      return environmentInfo;
    }

    String envId = (String) getArgumentValue(dataFetchingEnvironment, ENV_ID_ARG);
    if (StringUtils.isBlank(envId)) {
      addInvalidInputInfo(environmentInfo, GraphQLConstants.ENV_ID_ARG);
      return environmentInfo;
    }

    Environment env = environmentService.get(appId, envId);
    if (null == env) {
      addNoRecordFoundInfo(environmentInfo, ENV_ID_ARG);
      return environmentInfo;
    }
    return EnvironmentController.getEnvironmentInfo(env);
  }
}
