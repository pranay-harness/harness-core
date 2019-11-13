package io.harness.event.client;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.PublishMessage;
import io.harness.event.client.PublisherModule.Config;
import io.harness.event.client.PublisherModule.NoopEventPublisher;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PublisherModuleTest extends CategoryTest {
  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBindNoopPublisherIfPublishTargetIsNull() throws Exception {
    Injector injector = Guice.createInjector(new PublisherModule(Config.builder().build()));
    assertThat(injector.getInstance(EventPublisher.class))
        .isInstanceOfSatisfying(NoopEventPublisher.class,
            noopEventPublisher
            -> assertThatCode(() -> noopEventPublisher.publish(PublishMessage.newBuilder().build()))
                   .doesNotThrowAnyException());
  }
}