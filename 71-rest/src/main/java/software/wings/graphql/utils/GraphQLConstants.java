package software.wings.graphql.utils;

import lombok.experimental.UtilityClass;
import software.wings.beans.FeatureName;

@UtilityClass
public class GraphQLConstants {
  public static final String APP_ID_ARG = "applicationId";
  public static final String MAX_PAGE_SIZE_STR = "20";

  // Error messages
  public static final String FEATURE_NOT_ENABLED = FeatureName.GRAPHQL + " feature not enabled";
  public static final String RATE_LIMIT_REACHED =
      "You've reached your account's rate limit for data queries. Please try again later.";
  public static final String INVALID_API_KEY = "Invalid Api Key";
  public static final String INVALID_TOKEN = "Invalid Token";
}
