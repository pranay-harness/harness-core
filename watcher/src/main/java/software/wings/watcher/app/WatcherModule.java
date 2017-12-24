package software.wings.watcher.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import software.wings.utils.message.MessageService;
import software.wings.utils.message.MessageServiceImpl;
import software.wings.utils.message.MessengerType;
import software.wings.watcher.service.WatcherService;
import software.wings.watcher.service.WatcherServiceImpl;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 10/26/17
 */
public class WatcherModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(WatcherService.class).to(WatcherServiceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("inputExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("InputCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("watchExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Watch-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("upgradeExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Upgrade-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("commandCheckExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("CommandCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));

    int cores = Runtime.getRuntime().availableProcessors();
    bind(ExecutorService.class)
        .toInstance(new ThreadPoolExecutor(2 * cores, 20, (long) 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("watcher-task-%d").build()));
    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl(Clock.systemUTC(), MessengerType.WATCHER, WatcherApplication.getProcessId()));
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(AmazonS3Client.class)
        .toInstance((AmazonS3Client) AmazonS3ClientBuilder.standard()
                        .withRegion("us-east-1")
                        .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                        .build());
  }
}
