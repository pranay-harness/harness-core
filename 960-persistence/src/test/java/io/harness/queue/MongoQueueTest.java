/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.queue;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.joor.Reflect.on;

import io.harness.PersistenceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.mongo.queue.MongoQueueConsumer;
import io.harness.mongo.queue.MongoQueuePublisher;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queuable.QueuableKeys;
import io.harness.queue.QueueConsumer.Filter;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class MongoQueueTest extends PersistenceTestBase {
  private final Duration DEFAULT_WAIT = ofSeconds(3);
  private final Duration DEFAULT_POLL = ofSeconds(1);

  @Inject private HPersistence persistence;

  @Inject private QueuePublisher<TestTopicQueuableObject> topicProducer;
  @Inject private QueueConsumer<TestTopicQueuableObject> topicConsumer;

  private MongoQueueConsumer<TestTopicQueuableObject> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = (MongoQueueConsumer<TestTopicQueuableObject>) topicConsumer;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotGetMessageOnceAcquired() {
    topicProducer.send(new TestTopicQueuableObject(1));

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(Duration.ZERO, Duration.ZERO)).isNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReturnMessageInTimeOrder() {
    TestTopicQueuableObject messageOne = new TestTopicQueuableObject(1);
    TestTopicQueuableObject messageTwo = new TestTopicQueuableObject(2);
    TestTopicQueuableObject messageThree = new TestTopicQueuableObject(3);

    topicProducer.send(messageOne);
    topicProducer.send(messageTwo);
    topicProducer.send(messageThree);

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageOne);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageTwo);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageThree);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    Date start = new Date();
    queue.get(ofSeconds(1), DEFAULT_POLL);
    long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(1000L, 3000L);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    Date start = new Date();

    topicProducer.send(new TestTopicQueuableObject(1));

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    long delay = 100;
    for (int i = 0; i < 5; i++) {
      TestTopicQueuableObject message = new TestTopicQueuableObject(1);
      message.setEarliestGet(new Date(System.currentTimeMillis() + delay));
      topicProducer.send(message);

      if (queue.get(Duration.ZERO, Duration.ZERO) != null) {
        delay *= 2;
        continue;
      }

      sleep(ofMillis(delay + 1));

      assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
      return;
    }

    fail(format("Something seems wrong with this test. Delay %d should be enough.", delay));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldObtainStuckMessageWhenEarliestGetHasExpired() {
    topicProducer.send(new TestTopicQueuableObject(1));

    queue.setHeartbeat(ZERO);

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenTryToUpdateHeartbeatForNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.updateHeartbeat(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeatOfAlreadyExpiredMessage() {
    topicProducer.send(new TestTopicQueuableObject(1));
    TestTopicQueuableObject message = queue.get(Duration.ZERO, Duration.ZERO);

    queue.updateHeartbeat(message);

    TestTopicQueuableObject actual = persistence.get(TestTopicQueuableObject.class, message.getId());

    assertThat(actual.getEarliestGet()).isEqualTo(message.getEarliestGet());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeatOfMessageWhichIsNotRunning() {
    TestTopicQueuableObject message = new TestTopicQueuableObject(1);

    topicProducer.send(message);

    queue.updateHeartbeat(message);

    TestTopicQueuableObject actual = persistence.get(TestTopicQueuableObject.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldUpdateHeartbeatOfMessageWhileRunning() {
    queue.setHeartbeat(ofSeconds(10));
    topicProducer.send(new TestTopicQueuableObject(1));

    Date beforeGet = new Date();
    TestTopicQueuableObject message = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    // queue get returns the old timestamp. Update the message with the current values
    message = persistence.get(TestTopicQueuableObject.class, message.getId());

    Date messageEarliestGet = message.getEarliestGet();

    assertThat(messageEarliestGet).isAfter(beforeGet);
    queue.setHeartbeat(ofSeconds(20));
    queue.updateHeartbeat(message);

    TestTopicQueuableObject actual = persistence.get(TestTopicQueuableObject.class, message.getId());
    log.info("Actual Timestamp of message = {}", actual.getEarliestGet());

    assertThat(actual.getEarliestGet()).isAfter(messageEarliestGet);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(0);

    topicProducer.send(new TestTopicQueuableObject(1));

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldAckMessage() {
    topicProducer.send(new TestTopicQueuableObject(0));
    topicProducer.send(new TestTopicQueuableObject(1));

    TestTopicQueuableObject result = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    persistence.getDatastore(TestTopicQueuableObject.class);

    assertThat(persistence.createQuery(TestTopicQueuableObject.class).count()).isEqualTo(2);

    persistence.createQuery(TestTopicQueuableObject.class)
        .fetch()
        .forEach(dbObject -> log.debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(persistence.createQuery(TestTopicQueuableObject.class).count()).isEqualTo(1);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRequeueMessage() {
    TestTopicQueuableObject message = new TestTopicQueuableObject(0);

    topicProducer.send(message);

    TestTopicQueuableObject resultOne = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    Date expectedEarliestGet = new Date();
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne.getId(), 0, expectedEarliestGet);

    assertThat(persistence.createQuery(TestTopicQueuableObject.class).count()).isEqualTo(1);

    TestTopicQueuableObject actual = persistence.createQuery(TestTopicQueuableObject.class).get();

    TestTopicQueuableObject expected = new TestTopicQueuableObject(0);
    expected.setTopic("topic");
    expected.setEarliestGet(expectedEarliestGet);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, QueuableKeys.id, QueuableKeys.earliestGet);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenRequeuingWithNullEarliestGet() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(QueuableKeys.id, 0, null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> topicProducer.send(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSendMessage() {
    TestTopicQueuableObject message = new TestTopicQueuableObject(1);

    Date expectedEarliestGet = new Date();
    Date timeBeforeSend = new Date();
    message.setEarliestGet(expectedEarliestGet);
    topicProducer.send(message);

    assertThat(persistence.createQuery(TestTopicQueuableObject.class).count()).isEqualTo(1);

    TestTopicQueuableObject actual = persistence.createQuery(TestTopicQueuableObject.class).get();

    TestTopicQueuableObject expected = new TestTopicQueuableObject(1);
    expected.setTopic("topic");
    expected.setEarliestGet(expectedEarliestGet);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, QueuableKeys.id);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSendAndGetMessageWithEntityReference() {
    MongoQueuePublisher<TestQueuableWithEntity> entityProducer =
        new MongoQueuePublisher<>(TestQueuableWithEntity.class.getSimpleName(), null);
    on(entityProducer).set("persistence", persistence);

    MongoQueueConsumer<TestQueuableWithEntity> entityConsumer =
        new MongoQueueConsumer<>(TestQueuableWithEntity.class, ofSeconds(5), null);
    on(entityConsumer).set("persistence", persistence);

    TestInternalEntity testEntity = TestInternalEntity.builder().id("1").build();
    persistence.save(testEntity);

    TestQueuableWithEntity message = new TestQueuableWithEntity(testEntity);

    entityProducer.send(message);

    assertThat(persistence.createQuery(TestQueuableWithEntity.class).count()).isEqualTo(1);

    TestQueuableWithEntity actual = entityConsumer.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(actual.getEntity()).isEqualTo(testEntity);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldFilterWithTopic() {
    MongoQueuePublisher<TestTopicQueuableObject> topicPublisher =
        new MongoQueuePublisher<>(TestTopicQueuableObject.class.getSimpleName(), asList("topic1"));
    on(topicPublisher).set("persistence", persistence);

    MongoQueueConsumer<TestTopicQueuableObject> topicConsumer =
        new MongoQueueConsumer<>(TestTopicQueuableObject.class, ofSeconds(5), asList(asList("topic2")));
    on(topicConsumer).set("persistence", persistence);

    TestTopicQueuableObject message = new TestTopicQueuableObject(1);
    topicPublisher.send(message);
    assertThat(topicConsumer.get(DEFAULT_WAIT, DEFAULT_POLL)).isNull();
  }
}
