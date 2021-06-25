package io.harness.pms.sdk.core.events;

import static io.harness.eventsframework.EventsFrameworkConstants.DUMMY_REDIS_URL;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_INTERRUPT_EVENT_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_INTERRUPT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_TOPIC;
import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.manage.GlobalContextManager;
import io.harness.monitoring.MonitoringContext;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.pms.contracts.plan.Redis;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.PmsSdkCoreConfig;
import io.harness.redis.RedisConfig;
import io.harness.utils.RetryUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PipelineSdkEventSender {
  private static final RetryPolicy<Object> retryPolicy = RetryUtils.getRetryPolicy("Error Getting Producer..Retrying",
      "Failed to obtain producer", Collections.singletonList(ExecutionException.class), Duration.ofMillis(10), 3, log);
  @Inject private MongoTemplate mongoTemplate;
  @Inject private PmsSdkCoreConfig pmsSdkCoreConfig;

  private final LoadingCache<PmsEventCategory, Producer> producerCache =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterAccess(300, TimeUnit.MINUTES)
          .build(new CacheLoader<PmsEventCategory, Producer>() {
            @Override
            public Producer load(PmsEventCategory cacheKey) {
              return createProducer(cacheKey);
            }
          });

  public String sendEvent(ByteString eventData, PmsEventCategory eventCategory, boolean isMonitored) {
    log.info("Sending {} event for {} to the producer", eventCategory, ModuleType.PMS.name().toLowerCase());
    Producer producer = obtainProducer(eventCategory);

    ImmutableMap.Builder<String, String> metadataBuilder =
        ImmutableMap.<String, String>builder().put(SERVICE_NAME, ModuleType.PMS.name().toLowerCase());
    MonitoringContext monitoringContext = GlobalContextManager.get(MonitoringContext.IS_MONITORING_ENABLED);
    if (isMonitored && monitoringContext != null) {
      metadataBuilder.put(PIPELINE_MONITORING_ENABLED, String.valueOf(monitoringContext.isMonitoringEnabled()));
    }

    String messageId =
        producer.send(Message.newBuilder().putAllMetadata(metadataBuilder.build()).setData(eventData).build());
    log.info("Successfully Sent {} event for {} to the producer. MessageId {}", eventCategory,
        ModuleType.PMS.name().toLowerCase(), messageId);
    return messageId;
  }

  Producer obtainProducer(PmsEventCategory eventCategory) {
    Producer producer = Failsafe.with(retryPolicy).get(() -> producerCache.get(eventCategory));
    if (producer == null) {
      throw new RuntimeException("Cannot create Pipeline Sdk Event Framework producer.");
    }
    return producer;
  }

  Producer createProducer(PmsEventCategory pmsEventCategory) {
    switch (pmsEventCategory) {
      case SDK_RESPONSE_EVENT:
        return extractProducer(
            buildConsumerConfig(pmsSdkCoreConfig.getEventsFrameworkConfiguration(), pmsEventCategory),
            PIPELINE_SDK_RESPONSE_EVENT_MAX_TOPIC_SIZE);
      case SDK_INTERRUPT_EVENT_NOTIFY:
        return extractProducer(
            buildConsumerConfig(pmsSdkCoreConfig.getEventsFrameworkConfiguration(), pmsEventCategory),
            PIPELINE_SDK_INTERRUPT_EVENT_MAX_TOPIC_SIZE);
      default:
        throw new InvalidRequestException("Invalid Event Category while obtaining Producer in PMS SDK");
    }
  }

  private ConsumerConfig buildConsumerConfig(
      EventsFrameworkConfiguration eventsConfig, PmsEventCategory eventCategory) {
    RedisConfig redisConfig = eventsConfig.getRedisConfig();
    if (redisConfig != null) {
      return ConsumerConfig.newBuilder().setRedis(buildConsumerRedisConfig(eventCategory)).build();
    }
    throw new UnsupportedOperationException("Only Redis is Supported as Back End");
  }

  private static Redis buildConsumerRedisConfig(PmsEventCategory eventCategory) {
    switch (eventCategory) {
      case SDK_INTERRUPT_EVENT_NOTIFY:
        return Redis.newBuilder().setTopicName(PIPELINE_SDK_INTERRUPT_TOPIC).build();
      case SDK_RESPONSE_EVENT:
        return Redis.newBuilder().setTopicName(PIPELINE_SDK_RESPONSE_EVENT_TOPIC).build();
      default:
        throw new InvalidRequestException("Not a valid Event Category");
    }
  }

  private Producer extractProducer(ConsumerConfig consumerConfig, int topicSize) {
    ConsumerConfig.ConfigCase configCase = consumerConfig.getConfigCase();
    switch (configCase) {
      case REDIS:
        Redis redis = consumerConfig.getRedis();
        return buildRedisProducer(redis.getTopicName(),
            pmsSdkCoreConfig.getEventsFrameworkConfiguration().getRedisConfig(), pmsSdkCoreConfig.getServiceName(),
            topicSize);
      case CONFIG_NOT_SET:
      default:
        throw new InvalidRequestException("No producer found for Config Case " + configCase.name());
    }
  }

  private Producer buildRedisProducer(String topicName, RedisConfig redisConfig, String serviceId, int topicSize) {
    return redisConfig.getRedisUrl().equals(DUMMY_REDIS_URL)
        ? NoOpProducer.of(topicName)
        : RedisProducer.of(topicName, redisConfig, topicSize, serviceId);
  }
}
