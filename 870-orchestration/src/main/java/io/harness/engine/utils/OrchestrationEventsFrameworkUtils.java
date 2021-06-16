package io.harness.engine.utils;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.DUMMY_REDIS_URL;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.OrchestrationModuleConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.utils.ProducerCacheKey.EventCategory;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.pms.contracts.plan.ConsumerConfig.ConfigCase;
import io.harness.pms.contracts.plan.Redis;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstance.PmsSdkInstanceKeys;
import io.harness.redis.RedisConfig;
import io.harness.utils.RetryUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class OrchestrationEventsFrameworkUtils {
  private static final RetryPolicy<Object> retryPolicy = RetryUtils.getRetryPolicy("Error Getting Producer..Retrying",
      "Failed to obtain producer", Collections.singletonList(ExecutionException.class), Duration.ofMillis(10), 3, log);
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationModuleConfig moduleConfig;

  private final LoadingCache<ProducerCacheKey, Producer> producerCache =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterAccess(300, TimeUnit.MINUTES)
          .build(new CacheLoader<ProducerCacheKey, Producer>() {
            @Override
            public Producer load(ProducerCacheKey cacheKey) {
              return obtainProducer(cacheKey);
            }
          });

  public Producer obtainProducerForInterrupt(@NonNull String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.INTERRUPT_EVENT)
                                          .serviceName(serviceName)
                                          .build()));
  }

  public Producer obtainProducerForOrchestrationEvent(String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.ORCHESTRATION_EVENT)
                                          .serviceName(serviceName)
                                          .build()));
  }

  public Producer obtainProducerForFacilitationEvent(String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.FACILITATOR_EVENT)
                                          .serviceName(serviceName)
                                          .build()));
  }

  public Producer obtainProducerForNodeStart(String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.NODE_START)
                                          .serviceName(serviceName)
                                          .build()));
  }

  public Producer obtainProducerForProgressEvent(String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.PROGRESS_EVENT)
                                          .serviceName(serviceName)
                                          .build()));
  }

  public Producer obtainProducerForNodeAdviseEvent(String serviceName) {
    return Failsafe.with(retryPolicy)
        .get(()
                 -> producerCache.get(ProducerCacheKey.builder()
                                          .eventCategory(EventCategory.NODE_ADVISE)
                                          .serviceName(serviceName)
                                          .build()));
  }

  @VisibleForTesting
  Producer obtainProducer(ProducerCacheKey cacheKey) {
    PmsSdkInstance instance = getPmsSdkInstance(cacheKey.getServiceName());
    switch (cacheKey.getEventCategory()) {
      case INTERRUPT_EVENT:
        return extractProducer(
            instance.getInterruptConsumerConfig(), EventsFrameworkConstants.PIPELINE_INTERRUPT_EVENT_MAX_TOPIC_SIZE);
      case ORCHESTRATION_EVENT:
        return extractProducer(instance.getOrchestrationEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_ORCHESTRATION_EVENT_MAX_TOPIC_SIZE);
      case FACILITATOR_EVENT:
        return extractProducer(instance.getFacilitatorEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_FACILITATOR_EVENT_MAX_TOPIC_SIZE);
      case NODE_START:
        return extractProducer(instance.getNodeStartEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_NODE_START_EVENT_MAX_TOPIC_SIZE);
      case PROGRESS_EVENT:
        return extractProducer(
            instance.getProgressEventConsumerConfig(), EventsFrameworkConstants.PIPELINE_PROGRESS_MAX_TOPIC_SIZE);
      case NODE_ADVISE:
        return extractProducer(
            instance.getNodeAdviseEventConsumerConfig(), EventsFrameworkConstants.PIPELINE_NODE_ADVISE_MAX_TOPIC_SIZE);
      default:
        throw new InvalidRequestException("Invalid Event Category while obtaining Producer");
    }
  }

  private Producer extractProducer(ConsumerConfig consumerConfig, int topicSize) {
    ConfigCase configCase = consumerConfig.getConfigCase();
    switch (configCase) {
      case REDIS:
        Redis redis = consumerConfig.getRedis();
        return buildRedisProducer(redis.getTopicName(), moduleConfig.getEventsFrameworkConfiguration().getRedisConfig(),
            PIPELINE_SERVICE.getServiceId(), topicSize);
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

  PmsSdkInstance getPmsSdkInstance(String serviceName) {
    Query query = query(where(PmsSdkInstanceKeys.name).is(serviceName));
    PmsSdkInstance instance = mongoTemplate.findOne(query, PmsSdkInstance.class);
    if (instance == null) {
      throw new InvalidRequestException("Sdk Not registered for Service name" + serviceName);
    }
    return instance;
  }
}
