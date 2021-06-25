package io.harness.pms.sdk.core.events;

import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_INTERRUPT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_TOPIC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.PmsSdkCoreConfig;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.redis.RedisConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineSdkEventSenderTest extends PmsSdkCoreTestBase {
  @Inject PipelineSdkEventSender pipelineSdkEventSender;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestObtainProducer() {
    Producer producer = pipelineSdkEventSender.obtainProducer(PmsEventCategory.SDK_RESPONSE_EVENT);
    assertThat(((NoOpProducer) producer).getTopicName()).isEqualTo(PIPELINE_SDK_RESPONSE_EVENT_TOPIC);

    producer = pipelineSdkEventSender.obtainProducer(PmsEventCategory.SDK_INTERRUPT_EVENT_NOTIFY);
    assertThat(((NoOpProducer) producer).getTopicName()).isEqualTo(PIPELINE_SDK_INTERRUPT_TOPIC);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestCache() {
    PipelineSdkEventSender pipelineSdkEventSender = spy(PipelineSdkEventSender.class);
    Reflect.on(pipelineSdkEventSender)
        .set("pmsSdkCoreConfig",
            PmsSdkCoreConfig.builder()
                .eventsFrameworkConfiguration(EventsFrameworkConfiguration.builder()
                                                  .redisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
                                                  .build())
                .build());
    pipelineSdkEventSender.obtainProducer(PmsEventCategory.SDK_RESPONSE_EVENT);
    pipelineSdkEventSender.obtainProducer(PmsEventCategory.SDK_RESPONSE_EVENT);
    verify(pipelineSdkEventSender, times(1)).createProducer(any(PmsEventCategory.class));
  }
}