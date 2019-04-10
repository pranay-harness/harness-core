package io.harness.observer;

import static io.harness.observer.AsyncInformObserverTest.Sync.CALLBACK_IS_DONE;
import static io.harness.observer.AsyncInformObserverTest.Sync.FIRE_IS_DONE;
import static io.harness.observer.AsyncInformObserverTest.Sync.INITIAL;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncInformObserverTest {
  private static final Logger logger = LoggerFactory.getLogger(AsyncInformObserverTest.class);

  public interface ObserverProtocol { void method(); }

  enum Sync { INITIAL, FIRE_IS_DONE, CALLBACK_IS_DONE }

  public static class Observer implements ObserverProtocol, AsyncInformObserver {
    private ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    Sync sync = INITIAL;

    @Override
    public ExecutorService getInformExecutorService() {
      return executor;
    }

    @Override
    public void method() {
      synchronized (this) {
        while (sync != FIRE_IS_DONE) {
          try {
            wait();
          } catch (InterruptedException exception) {
            logger.error("", exception);
          }
        }
        sync = CALLBACK_IS_DONE;
        notifyAll();
      }
    }
  }

  private Observer observer;
  private Subject<ObserverProtocol> subject;

  @Before
  public void initialize() {
    observer = new Observer();
    subject = new Subject<>();
    subject.register(observer);
  }

  @Test(timeout = 1000)
  @Category(UnitTests.class)
  public void testResealingAsyncCall() throws InterruptedException {
    subject.fireInform(ObserverProtocol::method);

    synchronized (observer) {
      observer.sync = FIRE_IS_DONE;
      observer.notifyAll();
    }

    synchronized (observer) {
      while (observer.sync != CALLBACK_IS_DONE) {
        observer.wait();
      }
    }
  }
}
