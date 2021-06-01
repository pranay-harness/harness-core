package io.harness.engine.utils;

import static io.harness.pms.utils.PmsConstants.INTERNAL_SERVICE_NAME;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.pms.contracts.plan.Redis;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.MongoTemplate;

public class OrchestrationEventsFrameworkUtilsTest extends OrchestrationTestBase {
  @Inject OrchestrationEventsFrameworkUtils eventsFrameworkUtils;
  @Inject MongoTemplate mongoTemplate;

  private static final String TOPIC1 = "topic1";
  private static final String TOPIC2 = "topic2";

  @Before
  public void setup() {
    mongoTemplate.save(
        PmsSdkInstance.builder()
            .name(INTERNAL_SERVICE_NAME)
            .supportedTypes(new HashMap<>())
            .supportedSteps(new ArrayList<>())
            .supportedStepTypes(new ArrayList<>())
            .interruptConsumerConfig(
                ConsumerConfig.newBuilder().setRedis(Redis.newBuilder().setTopicName(TOPIC1).build()).build())
            .orchestrationEventConsumerConfig(
                ConsumerConfig.newBuilder().setRedis(Redis.newBuilder().setTopicName(TOPIC2).build()).build())
            .build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainProducerForInterrupt() {
    Producer producer = eventsFrameworkUtils.obtainProducerForInterrupt(INTERNAL_SERVICE_NAME);
    assertThat(((NoOpProducer) producer).getTopicName()).isEqualTo(TOPIC1);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainProducerForOrchestrationEvent() {
    Producer producer = eventsFrameworkUtils.obtainProducerForOrchestrationEvent(INTERNAL_SERVICE_NAME);
    assertThat(((NoOpProducer) producer).getTopicName()).isEqualTo(TOPIC2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCache() {
    OrchestrationEventsFrameworkUtils spyEventsFrameworkUtils = spy(OrchestrationEventsFrameworkUtils.class);
    Reflect.on(spyEventsFrameworkUtils)
        .set("moduleConfig",
            OrchestrationModuleConfig.builder()
                .serviceName(INTERNAL_SERVICE_NAME)
                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                .build());
    doReturn(PmsSdkInstance.builder()
                 .name(INTERNAL_SERVICE_NAME)
                 .supportedTypes(new HashMap<>())
                 .supportedSteps(new ArrayList<>())
                 .supportedStepTypes(new ArrayList<>())
                 .interruptConsumerConfig(
                     ConsumerConfig.newBuilder().setRedis(Redis.newBuilder().setTopicName(TOPIC1).build()).build())
                 .orchestrationEventConsumerConfig(
                     ConsumerConfig.newBuilder().setRedis(Redis.newBuilder().setTopicName(TOPIC2).build()).build())
                 .build())
        .when(spyEventsFrameworkUtils)
        .getPmsSdkInstance(INTERNAL_SERVICE_NAME);

    spyEventsFrameworkUtils.obtainProducerForOrchestrationEvent(INTERNAL_SERVICE_NAME);
    spyEventsFrameworkUtils.obtainProducerForOrchestrationEvent(INTERNAL_SERVICE_NAME);

    verify(spyEventsFrameworkUtils, times(1)).obtainProducer(any(ProducerCacheKey.class));
  }
}