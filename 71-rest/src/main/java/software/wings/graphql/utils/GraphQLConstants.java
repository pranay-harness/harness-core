package software.wings.graphql.utils;

import static software.wings.common.Constants.DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR;

import software.wings.beans.FeatureName;

public class GraphQLConstants {
  public static final String ENTITY_ID = "id";
  public static final String APP_ID = "appId";
  public static final String SERVICE_ID = "serviceId";
  public static final String ENV_ID = "envId";
  public static final String PAGE_LIMIT = "limit";
  public static final String PAGE_OFFSET = "offset";
  public static final String ZERO_OFFSET = "0";
  public static final String PAGE_SIZE_STR = DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR;
  public static final String FEATURE_NOT_ENABLED = FeatureName.GRAPHQL + " feature not enabled";
}
