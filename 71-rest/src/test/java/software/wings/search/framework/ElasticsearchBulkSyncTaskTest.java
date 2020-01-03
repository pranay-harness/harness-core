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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ElasticsearchBulkSyncTaskTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock private ElasticsearchSyncHelper elasticsearchSyncHelper;
  @Mock private ElasticsearchBulkMigrationHelper elasticsearchBulkMigrationHelper;
  @Inject @InjectMocks private ApplicationSearchEntity aSearchEntity;
  @Inject @InjectMocks private ElasticsearchBulkSyncTask elasticsearchBulkSyncTask;
  private final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBulkSyncProcess() {
    String newIndexName = "newIndexName";
    String oldIndexName = "oldIndexName";
    String oldVersion = "0.05";
    String newVersion = "0.1";
    ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob =
        ElasticsearchBulkMigrationJob.builder()
            .entityClass(aSearchEntity.getClass().getCanonicalName())
            .fromVersion(oldVersion)
            .toVersion(newVersion)
            .oldIndexName(oldIndexName)
            .newIndexName(newIndexName)
            .build();
    wingsPersistence.save(elasticsearchBulkMigrationJob);

    SearchEntityIndexState searchEntityIndexState =
        new SearchEntityIndexState(aSearchEntity.getClass().getCanonicalName(), "0.05", oldIndexName, false);
    wingsPersistence.save(searchEntityIndexState);

    Future f = threadPoolExecutor.submit(() -> {
      try {
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    String token = "token";
    String uuid = "uuid";
    String token1 = "token1";
    String uuid1 = "uuid1";
    ChangeEvent changeEvent = new ChangeEvent<>(token, ChangeType.DELETE, Application.class, uuid, null, null);
    ChangeEvent changeEvent1 = new ChangeEvent<>(token1, ChangeType.INSERT, Application.class, uuid1, null, null);

    when(elasticsearchIndexManager.deleteIndex(newIndexName)).thenReturn(true);

    doAnswer(invocationOnMock -> {
      ChangeSubscriber<?> changeSubscriber = invocationOnMock.getArgumentAt(0, ChangeSubscriber.class);
      changeSubscriber.onChange(changeEvent);
      changeSubscriber.onChange(changeEvent1);
      return f;
    })
        .when(elasticsearchSyncHelper)
        .startChangeListeners(any());

    when(elasticsearchBulkMigrationHelper.doBulkSync(any())).thenReturn(true);

    doAnswer(invocationOnMock -> {
      f.cancel(true);
      return null;
    })
        .when(elasticsearchSyncHelper)
        .stopChangeListeners();

    ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult = elasticsearchBulkSyncTask.run();

    assertThat(elasticsearchBulkSyncTaskResult.isSuccessful()).isEqualTo(true);
    Queue<ChangeEvent<?>> receivedChangeEvents = elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync();
    assertThat(receivedChangeEvents).isNotNull();
    assertThat(receivedChangeEvents.size()).isEqualTo(1);
    ChangeEvent<?> receivedChangeEvent = receivedChangeEvents.poll();
    assertThat(receivedChangeEvent).isNotNull();
    assertThat(receivedChangeEvent.getToken()).isEqualTo(token);
    assertThat(receivedChangeEvent.getUuid()).isEqualTo(uuid);

    verify(elasticsearchIndexManager, times(1)).deleteIndex(newIndexName);
    verify(elasticsearchSyncHelper, times(1)).startChangeListeners(any());
    verify(elasticsearchSyncHelper, times(1)).stopChangeListeners();

    ArgumentCaptor<Set<SearchEntity<?>>> captor = ArgumentCaptor.forClass((Class) Set.class);
    verify(elasticsearchBulkMigrationHelper, times(1)).doBulkSync(captor.capture());
    Set<SearchEntity<?>> capturedSearchEntities = captor.getValue();
    assertThat(capturedSearchEntities.size()).isEqualTo(6);

    ElasticsearchBulkMigrationJob shouldBeDeletedJob =
        wingsPersistence.get(ElasticsearchBulkMigrationJob.class, ApplicationSearchEntity.class.getCanonicalName());
    assertThat(shouldBeDeletedJob).isNull();

    SearchEntityIndexState createdSearchEntityIndexState =
        wingsPersistence.get(SearchEntityIndexState.class, aSearchEntity.getClass().getCanonicalName());
    assertThat(createdSearchEntityIndexState).isNotNull();
    assertThat(createdSearchEntityIndexState.getSyncVersion()).isEqualTo(ApplicationSearchEntity.VERSION);
    assertThat(createdSearchEntityIndexState.isRecreateIndex()).isEqualTo(false);
    assertThat(createdSearchEntityIndexState.shouldBulkSync()).isEqualTo(false);
  }
}
