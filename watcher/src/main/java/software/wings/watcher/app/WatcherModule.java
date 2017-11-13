package software.wings.watcher.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.message.MessageService;
import software.wings.utils.message.MessageServiceImpl;
import software.wings.utils.message.MessengerType;
import software.wings.watcher.service.UpgradeService;
import software.wings.watcher.service.UpgradeServiceImpl;
import software.wings.watcher.service.WatcherService;
import software.wings.watcher.service.WatcherServiceImpl;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 10/26/17
 */
public class WatcherModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(WatcherModule.class);

  @Override
  protected void configure() {
    bind(WatcherService.class).to(WatcherServiceImpl.class);
    int cores = Runtime.getRuntime().availableProcessors();
    bind(ExecutorService.class)
        .toInstance(new ThreadPoolExecutor(2 * cores, 50, (long) 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("watcher-task-%d").build()));
    bind(UpgradeService.class).to(UpgradeServiceImpl.class);
    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl(Clock.systemUTC(), MessengerType.WATCHER, WatcherApplication.getProcessId()));
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
  }
}
