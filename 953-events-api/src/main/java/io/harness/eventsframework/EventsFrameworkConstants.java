package io.harness.eventsframework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;

@OwnedBy(PL)
public final class EventsFrameworkConstants {
  public static final String ENTITY_CRUD = "entity_crud";
  public static final String FEATURE_FLAG_STREAM = "harness_internal_feature_flags";
  public static final String SETUP_USAGE = "setup_usage";
  public static final String ENTITY_ACTIVITY = "entity_activity";
  public static final String HARNESS_TO_GIT_PUSH = "harness_to_git_push";
  public static final String WEBHOOK_REQUEST_PAYLOAD_DETAILS = "webhook_request_payload_data";
  public static final String WEBHOOK_EVENTS_STREAM = "webhook_events_sream";
  public static final String GIT_PUSH_EVENT_STREAM = "git_push_event_stream";
  public static final String USERMEMBERSHIP = "usermembership";
  // created for git sdk, dont use outside sdk.
  public static final String GIT_CONFIG_STREAM = "git_config_stream";

  public static final String DUMMY_TOPIC_NAME = "dummy_topic_name";
  public static final String DUMMY_GROUP_NAME = "dummy_group_name";
  public static final String DUMMY_NAME = "dummy_name";

  public static final int DEFAULT_TOPIC_SIZE = 1000000;
  public static final int ENTITY_CRUD_MAX_TOPIC_SIZE = 1000000;
  public static final int FEATURE_FLAG_MAX_TOPIC_SIZE = 1000000;
  public static final int SETUP_USAGE_MAX_TOPIC_SIZE = 1000000;
  public static final int ENTITY_ACTIVITY_MAX_TOPIC_SIZE = 10000;
  public static final int HARNESS_TO_GIT_PUSH_MAX_TOPIC_SIZE = 1000000;
  public static final int WEBHOOK_REQUEST_PAYLOAD_DETAILS_MAX_TOPIC_SIZE = 1000000;
  public static final int WEBHOOK_EVENTS_STREAM_MAX_TOPIC_SIZE = 1000000;
  public static final int GIT_PUSH_EVENT_STREAM_MAX_TOPIC_SIZE = 1000000;
  public static final int GIT_CONFIG_STREAM_MAX_TOPIC_SIZE = 1000000;

  public static final Duration DEFAULT_MAX_PROCESSING_TIME = Duration.ofSeconds(10);
  public static final Duration ENTITY_CRUD_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration WEBHOOK_EVENTS_STREAM_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration FEATURE_FLAG_MAX_PROCESSING_TIME = Duration.ofMinutes(10);
  public static final Duration SETUP_USAGE_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration ENTITY_ACTIVITY_MAX_PROCESSING_TIME = Duration.ofSeconds(20);
  public static final Duration HARNESS_TO_GIT_PUSH_MAX_PROCESSING_TIME = Duration.ofSeconds(50);
  public static final Duration GIT_CONFIG_STREAM_PROCESSING_TIME = Duration.ofSeconds(20);

  public static final int DEFAULT_READ_BATCH_SIZE = 50;
  public static final int ENTITY_CRUD_READ_BATCH_SIZE = 50;
  public static final int FEATURE_FLAG_READ_BATCH_SIZE = 50;
  public static final int SETUP_USAGE_READ_BATCH_SIZE = 50;
  public static final int ENTITY_ACTIVITY_READ_BATCH_SIZE = 50;
  public static final int HARNESS_TO_GIT_PUSH_READ_BATCH_SIZE = 50;
  public static final int GIT_CONFIG_STREAM_READ_BATCH_SIZE = 50;
  public static final int WEBHOOK_EVENTS_STREAM_BATCH_SIZE = 50;
  public static final int GIT_PUSH_EVENT_STREAM_BATCH_SIZE = 50;
}
