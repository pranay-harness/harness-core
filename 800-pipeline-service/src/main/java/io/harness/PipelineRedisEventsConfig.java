package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PLAN_NOTIFY_EVENT_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.SETUP_USAGE_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_REQUEST_PAYLOAD_DETAILS_MAX_TOPIC_SIZE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class PipelineRedisEventsConfig {
  @Default RedisEventConfig setupUsage = RedisEventConfig.builder().maxTopicSize(SETUP_USAGE_MAX_TOPIC_SIZE).build();
  @Default
  RedisEventConfig planNotifyEvent = RedisEventConfig.builder().maxTopicSize(PLAN_NOTIFY_EVENT_MAX_TOPIC_SIZE).build();
  @Default
  RedisEventConfig webhookPayloadDetails =
      RedisEventConfig.builder().maxTopicSize(WEBHOOK_REQUEST_PAYLOAD_DETAILS_MAX_TOPIC_SIZE).build();
  @Default RedisEventConfig entityCrud = RedisEventConfig.builder().maxTopicSize(ENTITY_CRUD_MAX_TOPIC_SIZE).build();
  @Default
  RedisEventConfig orchestrationNotifyEvent =
      RedisEventConfig.builder().maxTopicSize(PMS_ORCHESTRATION_NOTIFY_EVENT_MAX_TOPIC_SIZE).build();
}
