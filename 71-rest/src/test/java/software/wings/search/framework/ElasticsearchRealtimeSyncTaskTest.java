package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ElasticsearchRealtimeSyncTaskTest extends WingsBaseTest {
  private final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
  @Mock private ElasticsearchSyncHelper elasticsearchSyncHelper;
  @Mock private ChangeEventProcessor changeEventProcessor;
  @Inject @InjectMocks private ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testRealtimeSyncProcess() throws InterruptedException, ExecutionException, TimeoutException {
    String token = "token";
    String uuid = "uuid";
    String token1 = "token1";
    String uuid1 = "uuid1";
    ChangeEvent changeEvent = new ChangeEvent<>(token, ChangeType.DELETE, Application.class, uuid, null, null);
    ChangeEvent changeEvent1 = new ChangeEvent<>(token1, ChangeType.INSERT, Application.class, uuid1, null, null);
    CountDownLatch latch = new CountDownLatch(1);

    Queue<ChangeEvent<?>> changeEvents = new LinkedList<>();
    changeEvents.add(changeEvent1);

    doAnswer(invocationOnMock -> {
      ChangeSubscriber<?> changeSubscriber = invocationOnMock.getArgumentAt(0, ChangeSubscriber.class);
      changeSubscriber.onChange(changeEvent);
      latch.countDown();
      return null;
    })
        .when(elasticsearchSyncHelper)
        .startChangeListeners(any());

    when(elasticsearchSyncHelper.checkIfAnyChangeListenerIsAlive()).thenReturn(true);

    Future realTimeSyncFuture = threadPoolExecutor.submit(() -> {
      try {
        elasticsearchRealtimeSyncTask.run(changeEvents);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    assertThat(realTimeSyncFuture.isDone()).isFalse();

    latch.await(10000, TimeUnit.SECONDS);
    verify(elasticsearchSyncHelper, times(1)).startChangeListeners(any());
    verify(changeEventProcessor, times(2)).processChangeEvent(any());

    elasticsearchRealtimeSyncTask.stop();
    realTimeSyncFuture.get(10000, TimeUnit.SECONDS);
    verify(elasticsearchSyncHelper, times(2)).stopChangeListeners();
  }
}
