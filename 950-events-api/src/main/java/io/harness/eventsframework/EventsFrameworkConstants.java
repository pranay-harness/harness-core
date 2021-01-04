package io.harness.eventsframework;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class EventsFrameworkConstants {
  public static final String ENTITY_CRUD = "entity_crud";
  public static final String ENTITY_TYPE_METADATA = "entityType";
  public static final String FEATURE_FLAG_STREAM = "harness_internal_feature_flags";

  public static final String ACTION_METADATA = "action";
  public static final String CREATE_ACTION = "create";
  public static final String UPDATE_ACTION = "update";
  public static final String DELETE_ACTION = "delete";

  public static final String PROJECT_ENTITY = "project";
  public static final String ORGANIZATION_ENTITY = "organization";
  public static final String CONNECTOR_ENTITY = "connector";
  public static final String SETUP_USAGE_ENTITY = "setupUsage";
  public static final String ACTIVITY_ENTITY = "entityActivity";
  public static final String ACCOUNT_ENTITY = "account";

  public static final String SETUP_USAGE_CREATE = "setup_usage_create";
  public static final String SETUP_USAGE_DELETE = "setup_usage_delete";
  public static final String ENTITY_ACTIVITY_CREATE = "entity_activity_create";

  public static final String DUMMY_TOPIC_NAME = "dummy_topic_name";
  public static final String DUMMY_GROUP_NAME = "dummy_group_name";
  public static final String DUMMY_NAME = "dummy_name";

  public static final int ENTITY_CRUD_MAX_TOPIC_SIZE = 100000;
  public static final int FEATURE_FLAG_MAX_TOPIC_SIZE = 100000;

  public static final Duration ENTITY_CRUD_MAX_PROCESSING_TIME = Duration.ofMinutes(1);
  public static final Duration FEATURE_FLAG_MAX_PROCESSING_TIME = Duration.ofMinutes(1);
}
