package io.harness.event.client.impl.appender;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;
import io.harness.event.client.impl.appender.AppenderModule.Config;
import io.harness.event.client.impl.appender.AppenderModule.NoopEventPublisher;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppenderModuleTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBindNoopPublisherIfNoQueueFileConfigured() throws Exception {
    Injector injector = Guice.createInjector(new AppenderModule(new Config(null), () -> ""));
    Assertions.assertThat(injector.getInstance(EventPublisher.class))
        .isInstanceOfSatisfying(NoopEventPublisher.class,
            noopEventPublisher
            -> assertThatCode(() -> noopEventPublisher.publish(PublishMessage.newBuilder().build()))
                   .doesNotThrowAnyException());
  }
}
