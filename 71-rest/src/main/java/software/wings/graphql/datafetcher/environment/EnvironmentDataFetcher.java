package software.wings.graphql.datafetcher.environment;

import static software.wings.graphql.utils.GraphQLConstants.APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.ENV_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.EnvironmentInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentDataFetcher extends AbstractDataFetcher<EnvironmentInfo> {
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

  @Override
  public EnvironmentInfo get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    EnvironmentInfo environmentInfo = EnvironmentInfo.builder().build();
    String appId = (String) getArgumentValue(dataFetchingEnvironment, APP_ID);
    // Pre-checks
    if (StringUtils.isBlank(appId)) {
      addInvalidInputInfo(environmentInfo, GraphQLConstants.APP_ID);
      return environmentInfo;
    }

    String envId = (String) getArgumentValue(dataFetchingEnvironment, ENV_ID);
    if (StringUtils.isBlank(envId)) {
      addInvalidInputInfo(environmentInfo, GraphQLConstants.ENV_ID);
      return environmentInfo;
    }

    Environment env = environmentService.get(appId, envId);
    if (null == env) {
      addNoRecordFoundInfo(environmentInfo, ENV_ID);
      return environmentInfo;
    }
    return environmentAdaptor.getEnvironmentInfo(env);
  }
}
