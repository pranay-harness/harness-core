package software.wings.search.framework.changestreams;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import com.mongodb.MongoInterruptedException;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class ChangeTrackingTaskTest extends WingsBaseTest {
  @Inject private MainConfiguration mainConfiguration;
  private final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testChangeTrackingTask() throws InterruptedException, ExecutionException {
    ChangeStreamSubscriber changeStreamSubscriber = mock(ChangeStreamSubscriber.class);
    MongoCollection<DBObject> collection = (MongoCollection<DBObject>) mock(MongoCollection.class);
    CountDownLatch latch = mock(CountDownLatch.class);
    ChangeStreamIterable<DBObject> changeStreamIterable =
        (ChangeStreamIterable<DBObject>) mock(ChangeStreamIterable.class);

    CountDownLatch testLatch = new CountDownLatch(1);
    String token =
        "{ \"_data\" : { \"$binary\" : \"gl2kjvIAAAAGRjxfaWQAPEEwckctZFoyVDhTMk1ySzcxZlpOZGcAAFoQBJxC1VIDm0XTvvbRGu0X6pUE\", \"$type\" : \"00\" } }";

    ChangeTrackingTask changeTrackingTask =
        new ChangeTrackingTask(changeStreamSubscriber, collection, null, latch, token);

    mockChangeTrackingTaskDependencies(changeStreamSubscriber, collection, latch, changeStreamIterable, testLatch);

    Future f = threadPoolExecutor.submit(changeTrackingTask);
    testLatch.await(50, TimeUnit.SECONDS);

    verify(collection, times(1)).watch();
    verify(changeStreamIterable, times(1)).fullDocument(FullDocument.UPDATE_LOOKUP);
    verify(changeStreamIterable, times(1)).maxAwaitTime(1, TimeUnit.MINUTES);
    verify(changeStreamSubscriber, times(1)).onChange(any());
    assertThat(f.isDone()).isEqualTo(false);

    threadPoolExecutor.shutdownNow();
    threadPoolExecutor.awaitTermination(50, TimeUnit.SECONDS);
    verify(latch, times(1)).countDown();
  }

  private void mockChangeTrackingTaskDependencies(ChangeStreamSubscriber changeStreamSubscriber,
      MongoCollection<DBObject> collection, CountDownLatch latch, ChangeStreamIterable<DBObject> changeStreamIterable,
      CountDownLatch testLatch) {
    MongoCursor<ChangeStreamDocument<DBObject>> mongoCursor = mock(MongoCursor.class);

    doNothing().when(changeStreamSubscriber).onChange(any());
    doNothing().when(latch).countDown();
    when(collection.watch()).thenReturn(changeStreamIterable);
    when(changeStreamIterable.fullDocument(any(FullDocument.class))).thenReturn(changeStreamIterable);
    when(changeStreamIterable.maxAwaitTime(any(Integer.class), any(TimeUnit.class))).thenReturn(changeStreamIterable);
    when(changeStreamIterable.resumeAfter(any(BsonDocument.class))).thenReturn(changeStreamIterable);
    when(changeStreamIterable.iterator()).thenReturn(mongoCursor);
    when(mongoCursor.getServerAddress()).thenReturn(new ServerAddress());
    when(mongoCursor.getServerCursor()).thenReturn(new ServerCursor(1, new ServerAddress()));

    doAnswer(i -> {
      try {
        ChangeStreamDocument<DBObject> changeStreamDocument =
            new ChangeStreamDocument<>(null, null, null, null, null, null);
        Consumer<ChangeStreamDocument<DBObject>> changeStreamSubscriberConsumer = i.getArgumentAt(0, Consumer.class);
        changeStreamSubscriberConsumer.accept(changeStreamDocument);
        testLatch.countDown();
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {
        throw new MongoInterruptedException("msg", e);
      }
      return null;
    })
        .when(mongoCursor)
        .forEachRemaining((Consumer<ChangeStreamDocument<DBObject>>) notNull());
  }
}
