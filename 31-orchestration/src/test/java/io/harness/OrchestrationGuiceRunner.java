package io.harness;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import io.harness.delay.DelayEventListener;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ServersModule;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.rule.LifecycleRule;
import io.harness.runners.GuiceRunner;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import lombok.SneakyThrows;
import org.junit.runners.model.InitializationError;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class OrchestrationGuiceRunner extends GuiceRunner {

  private transient ClosingFactory closingFactory;

  public OrchestrationGuiceRunner(Class<?> klass, Injector injector) throws Exception {
    super(klass, injector);
    this.closingFactory = new LifecycleRule().getClosingFactory();
    init(klass);
  }

  public OrchestrationGuiceRunner(Class<?> klass) throws Exception {
    super(klass);
    this.closingFactory = new LifecycleRule().getClosingFactory();
    init(klass);
  }

  private void init(Class<?> klass) throws IllegalAccessException, InitializationError, InstantiationException {
    for (Module module : getModulesFor(klass)) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }

    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(OrchestrationNotifyEventListener.class), 1);
    queueListenerController.register(injector.getInstance(DelayEventListener.class), 1);

    closingFactory.addServer(new Closeable() {
      @SneakyThrows
      @Override
      public void close() {
        queueListenerController.stop();
      }
    });

    final QueuePublisher<NotifyEvent> publisher =
            injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
            injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(
            ORCHESTRATION, payload -> publisher.send(Collections.singletonList(ORCHESTRATION), payload));

    NotifierScheduledExecutorService notifierScheduledExecutorService =
            injector.getInstance(NotifierScheduledExecutorService.class);
    notifierScheduledExecutorService.scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), 0L, 1000L, TimeUnit.MILLISECONDS);
    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        notifierScheduledExecutorService.shutdown();
      }
    });
  }

}
