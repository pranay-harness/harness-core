package io.harness.pms.sdk.core.execution.events.base;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.eventsframework.consumer.Message;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.events.PmsEventFrameworkConstants;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.utils.PmsConstants;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SdkBaseEventMessageListenerTest extends PmsSdkCoreTestBase {
  @Inject private Injector injector;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForPms() {
    NoopSdkSdkBaseEventMessageListener noopListener = injector.getInstance(NoopSdkSdkBaseEventMessageListener.class);
    Reflect.on(noopListener).set("serviceName", PmsConstants.INTERNAL_SERVICE_NAME);
    boolean processable = noopListener.isProcessable(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, PmsConstants.INTERNAL_SERVICE_NAME)
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(processable).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForNull() {
    NoopSdkSdkBaseEventMessageListener noopListener = injector.getInstance(NoopSdkSdkBaseEventMessageListener.class);
    boolean processable = noopListener.isProcessable(Message.newBuilder().build());
    assertThat(processable).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForService() {
    NoopSdkSdkBaseEventMessageListener noopListener = injector.getInstance(NoopSdkSdkBaseEventMessageListener.class);
    boolean processable = noopListener.isProcessable(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, PMS_SDK_CORE_SERVICE_NAME)
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(processable).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForDiffService() {
    NoopSdkSdkBaseEventMessageListener noopListener = injector.getInstance(NoopSdkSdkBaseEventMessageListener.class);
    boolean processable = noopListener.isProcessable(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, "RANDOM_SERVICE")
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(processable).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestExtractEntity() {
    NoopSdkSdkBaseEventMessageListener noopListener = injector.getInstance(NoopSdkSdkBaseEventMessageListener.class);
    InterruptEvent event = noopListener.extractEntity(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, PMS_SDK_CORE_SERVICE_NAME)
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(event).isNotNull();
    assertThat(event.getType()).isEqualTo(InterruptType.ABORT);
  }
}