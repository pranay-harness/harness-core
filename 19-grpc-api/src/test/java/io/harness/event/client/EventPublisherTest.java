package io.harness.event.client;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;
import com.google.protobuf.Any;

import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.client.PublisherModule.Config;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.OwnerRule.Owner;
import io.harness.threading.Concurrent;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EventPublisherTest extends CategoryTest {
  private static final String QUEUE_FILE_PATH = FileUtils.getTempDirectoryPath() + "/" + UUID.randomUUID().toString();
  private static final String SERVER_NAME = InProcessServerBuilder.generateName();
  private final AtomicInteger messagesPublished = new AtomicInteger();

  private Injector injector = Guice.createInjector(Modules
                                                       .override(new PublisherModule(Config.builder()
                                                                                         .publishTarget("NOT_USED")
                                                                                         .publishAuthority("NOT_USED")
                                                                                         .queueFilePath(QUEUE_FILE_PATH)
                                                                                         .build()))
                                                       .with(new AbstractModule() {
                                                         @Override
                                                         protected void configure() {}

                                                         @Provides
                                                         @Singleton
                                                         EventPublisherBlockingStub eventPublisherBlockingStub() {
                                                           return EventPublisherGrpc.newBlockingStub(
                                                               InProcessChannelBuilder.forName(SERVER_NAME).build());
                                                         }
                                                       }));
  @Inject private EventPublisherBlockingStub blockingStub;
  @Inject private RollingChronicleQueue chronicleQueue;
  @Inject private FileDeletionManager fileDeletionManager;
  private Server server;
  private FakeService fakeService;
  private EventPublisher eventPublisher;

  @Before
  public void setUp() throws Exception {
    File directory = new File(QUEUE_FILE_PATH);
    FileUtils.forceMkdir(directory);
    FileUtils.cleanDirectory(directory);
    injector.injectMembers(this);
    messagesPublished.set(0);
    fakeService = new FakeService();
    eventPublisher = new EventPublisherChronicleImpl(blockingStub, chronicleQueue, fileDeletionManager);
    server = InProcessServerBuilder.forName(SERVER_NAME).addService(fakeService).build();
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.cleanDirectory(new File(QUEUE_FILE_PATH));
    eventPublisher.shutdown();
    server.shutdown();
    server.awaitTermination();
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldThrowIaeIfWrongMethodIsCalled() {
    assertThatIllegalArgumentException().isThrownBy(
        ()
            -> eventPublisher.publishMessage(
                PublishMessage.newBuilder().build(), HTimestamps.fromInstant(Instant.now())));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void testNoMessageLoss() throws Exception {
    concurrentPublish(eventPublisher);
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false, intermittent = true)
  @Category(UnitTests.class)
  public void testNoMessageLossWithErrorProneServer() throws Exception {
    fakeService.setErrorProne(true);
    concurrentPublish(eventPublisher);
  }

  private void concurrentPublish(EventPublisher eventPublisher) throws Exception {
    int numThreads = 20;
    CountDownLatch latch = new CountDownLatch(numThreads);
    Concurrent.test(numThreads, x -> {
      try {
        int numMessages = 1000;
        for (int i = 0; i < numMessages; i++) {
          eventPublisher.publishMessage(Lifecycle.newBuilder()
                                            .setInstanceId("instanceId-123")
                                            .setType(EVENT_TYPE_START)
                                            .setTimestamp(HTimestamps.fromInstant(Instant.now()))
                                            .build(),
              HTimestamps.fromInstant(Instant.now()));
          messagesPublished.incrementAndGet();
        }
      } finally {
        latch.countDown();
      }
    });
    latch.await();
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        // >= because "at-least"-once delivery semantics - i.e. duplicate delivery is allowed.
        .until(() -> assertThat(fakeService.getMessageCount()).isGreaterThanOrEqualTo(messagesPublished.get()));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void testMessageReceived() {
    fakeService.setRecordMessages(true);
    Instant instant = Instant.now().minus(3, ChronoUnit.MINUTES);
    PublishMessage publishMessage =
        PublishMessage.newBuilder()
            .setPayload(Any.pack(ecsLifecycleEvent("instance-123", instant, EVENT_TYPE_STOP)))
            .build();
    eventPublisher.publish(publishMessage);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until(() -> assertThat(fakeService.getReceivedMessages()).contains(publishMessage));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void testMessageReceivedIfServerCallFails() {
    fakeService.setRecordMessages(true);
    fakeService.failNext();

    Instant instant = Instant.now().minus(13, ChronoUnit.MINUTES);
    PublishMessage publishMessage =
        PublishMessage.newBuilder()
            .setPayload(Any.pack(ecsLifecycleEvent("instance-456", instant, EVENT_TYPE_START)))
            .build();

    eventPublisher.publish(publishMessage);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until(() -> assertThat(fakeService.getReceivedMessages()).contains(publishMessage));
  }

  private Lifecycle ecsLifecycleEvent(String instanceId, Instant eventTime, EventType eventType) {
    return Lifecycle.newBuilder()
        .setInstanceId(instanceId)
        .setTimestamp(HTimestamps.fromInstant(eventTime))
        .setType(eventType)
        .setCreatedTimestamp(HTimestamps.fromInstant(Instant.now()))
        .build();
  }
}
