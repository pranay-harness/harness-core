package io.harness.pms.events.base;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.consumer.Message;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.events.PmsEventFrameworkConstants;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SdkBaseEventMessageListenerTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForNull() {
    NoopSdkSdkBaseEventMessageListener noopListener = new NoopSdkSdkBaseEventMessageListener("RANDOM_SERVICE");
    boolean processable = noopListener.isProcessable(Message.newBuilder().build());
    assertThat(processable).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForService() {
    NoopSdkSdkBaseEventMessageListener noopListener = new NoopSdkSdkBaseEventMessageListener("RANDOM_SERVICE");
    boolean processable = noopListener.isProcessable(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, "RANDOM_SERVICE")
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(processable).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsProcessableForDiffService() {
    NoopSdkSdkBaseEventMessageListener noopListener = new NoopSdkSdkBaseEventMessageListener("CD");
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
    NoopSdkSdkBaseEventMessageListener noopListener = new NoopSdkSdkBaseEventMessageListener("CD");
    InterruptEvent event = noopListener.extractEntity(
        Message.newBuilder()
            .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                            .putMetadata(PmsEventFrameworkConstants.SERVICE_NAME, "CD")
                            .setData(InterruptEvent.newBuilder().setType(InterruptType.ABORT).build().toByteString())
                            .build())
            .build());
    assertThat(event).isNotNull();
    assertThat(event.getType()).isEqualTo(InterruptType.ABORT);
  }
}