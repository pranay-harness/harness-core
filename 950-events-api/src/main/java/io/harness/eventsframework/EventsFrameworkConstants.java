package io.harness.eventsframework;

import java.time.Duration;
public final class EventsFrameworkConstants {
  public static final String ENTITY_CRUD = "entity_crud";
  public static final String FEATURE_FLAG_STREAM = "harness_internal_feature_flags";

  public static final String DUMMY_TOPIC_NAME = "dummy_topic_name";
  public static final String DUMMY_GROUP_NAME = "dummy_group_name";
  public static final String DUMMY_NAME = "dummy_name";

  public static final int ENTITY_CRUD_MAX_TOPIC_SIZE = 100000;
  public static final int FEATURE_FLAG_MAX_TOPIC_SIZE = 100000;

  public static final Duration ENTITY_CRUD_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration FEATURE_FLAG_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
}
