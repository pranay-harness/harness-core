package io.harness.eventsframework;

import java.time.Duration;
public final class EventsFrameworkConstants {
  public static final String ENTITY_CRUD = "entity_crud";
  public static final String FEATURE_FLAG_STREAM = "harness_internal_feature_flags";
  public static final String SETUP_USAGE = "setup_usage";
  public static final String ENTITY_ACTIVITY = "entity_activity";
  public static final String USER_ACCOUNT_MEMBERSHIP = "user_account_membership";

  public static final String DUMMY_TOPIC_NAME = "dummy_topic_name";
  public static final String DUMMY_GROUP_NAME = "dummy_group_name";
  public static final String DUMMY_NAME = "dummy_name";

  public static final int DEFAULT_TOPIC_SIZE = 1000000;
  public static final int ENTITY_CRUD_MAX_TOPIC_SIZE = 1000000;
  public static final int FEATURE_FLAG_MAX_TOPIC_SIZE = 1000000;
  public static final int SETUP_USAGE_MAX_TOPIC_SIZE = 1000000;
  public static final int ENTITY_ACTIVITY_MAX_TOPIC_SIZE = 1000000;

  public static final Duration ENTITY_CRUD_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration FEATURE_FLAG_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration SETUP_USAGE_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration ENTITY_ACTIVITY_MAX_PROCESSING_TIME = Duration.ofSeconds(20);

  public static final int ENTITY_CRUD_READ_BATCH_SIZE = 50;
  public static final int FEATURE_FLAG_READ_BATCH_SIZE = 50;
  public static final int SETUP_USAGE_READ_BATCH_SIZE = 50;
  public static final int ENTITY_ACTIVITY_READ_BATCH_SIZE = 50;
}
