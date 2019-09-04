package software.wings.delegatetasks.cv;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import io.harness.threading.ThreadPool;
import org.junit.Before;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DataCollectionTestBase {
  protected Injector injector = Guice.createInjector(new AbstractModule() {
    @Override
    protected void configure() {
      bind(ExecutorService.class)
          .annotatedWith(Names.named("verificationDataCollector"))
          .toInstance(ThreadPool.create(4, 20, 5, TimeUnit.SECONDS,
              new ThreadFactoryBuilder()
                  .setNameFormat("Verification-Data-Collector-%d")
                  .setPriority(Thread.MIN_PRIORITY)
                  .build()));
    }
  });

  @Before
  public void setup() {
    injector.injectMembers(this);
  }
}
