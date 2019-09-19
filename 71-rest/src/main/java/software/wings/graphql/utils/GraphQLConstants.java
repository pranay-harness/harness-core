package software.wings.graphql.utils;

import software.wings.beans.FeatureName;

public class GraphQLConstants {
  public static final String APP_ID_ARG = "applicationId";
  public static final String MAX_PAGE_SIZE_STR = "20";

  // Error messages
  public static final String FEATURE_NOT_ENABLED = FeatureName.GRAPHQL + " feature not enabled";
  public static final String EXTERNAL_RATE_LIMIT_REACHED =
      "External " + FeatureName.GRAPHQL + " API rate limit reached for account %s";
  public static final String INTERNAL_RATE_LIMIT_REACHED =
      "Internal " + FeatureName.GRAPHQL + " API rate limit reached for account %s";
  public static final String INVALID_API_KEY = "Invalid Api Key";
}
