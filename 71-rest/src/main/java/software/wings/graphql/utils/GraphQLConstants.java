package software.wings.graphql.utils;

import software.wings.beans.FeatureName;

public class GraphQLConstants {
  public static final String APP_ID_ARG = "appId";
  public static final String ACCOUNT_ID_ARG = "accountId";
  public static final String SERVICE_ID_ARG = "serviceId";
  public static final String INSTANCE_COUNT_TYPE_ARG = "instanceCountType";
  public static final String ENV_ID_ARG = "envId";
  public static final String PAGE_LIMIT_ARG = "limit";
  public static final String PAGE_OFFSET_ARG = "offset";
  public static final int ZERO_OFFSET = 0;
  public static final String ZERO_OFFSET_STR = "0";
  public static final int MAX_PAGE_SIZE = 20;
  public static final String MAX_PAGE_SIZE_STR = "20";
  public static final String ENV_TYPE_ARG = "environmentType";

  // Workflow related constants.
  public static final String WORKFLOW_ID = "workflowId";

  // Error messages
  public static final String FEATURE_NOT_ENABLED = FeatureName.GRAPHQL + " feature not enabled";
  public static final String EMPTY_OR_NULL_INPUT_FIELD = "Input field %s is either null or empty";
  public static final String USER_NOT_AUTHORIZED_TO_VIEW_ENTITY =
      "User is not authorized to view entity %s = %s  for appId = %s";
  public static final String NO_RECORDS_FOUND_FOR_APP_ID = "No records found for %s entity appId = %s";
  public static final String NO_RECORDS_FOUND_FOR_APP_ID_AND_ENTITY =
      "No records found for %s entity for id %s and appId = %s";
  public static final String NO_RECORDS_FOUND_FOR_ENTITIES =
      "No records found for %s entity for appId = %s, envId %s and serviceId=%s";
  public static final String NO_RECORDS_FOUND_FOR_APP_ENV_AND_WORKFLOW =
      "No records found for %s entity for appId = %s, envId %s and workflowId=%s";
}
